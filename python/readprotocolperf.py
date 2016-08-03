import base64
from collections import defaultdict
import os
import select
import socket
from statistics import mean, stdev
import struct
import time
from threading import Thread
from traceback import print_exc


ESCAPE_CHAR = "\\"

END = "e"


BYTE_TYPE = 0
STRING_TYPE = 1
COMMAND_TYPE = 2
INT_TYPE = 3
LONG_TYPE = 4
DOUBLE_TYPE = 5

ITERATIONS = 50000

LENGTH_TYPES = (BYTE_TYPE, STRING_TYPE, COMMAND_TYPE)


def unescape_new_line(escaped):
    """Replaces escaped characters by unescaped characters.

    For example, double backslashes are replaced by a single backslash.

    The behavior for improperly formatted strings is undefined and can change.

    :param escaped: the escaped string

    :rtype: the original string
    """
    return ESCAPE_CHAR.join(
        "\n".join(
            ("\r".join(p.split(ESCAPE_CHAR + "r")))
            .split(ESCAPE_CHAR + "n"))
        for p in escaped.split(ESCAPE_CHAR + ESCAPE_CHAR))


def quiet_close(closable):
    """Quietly closes a closable object without throwing an exception.

    :param closable: Object with a ``close`` method.
    """
    if closable is None:
        # Do not attempt to close a None. This logs unecessary exceptions.
        return

    try:
        closable.close()
    except Exception:
        pass


def set_reuse_address(server_socket):
    """Sets reuse address option if not on windows.

    On windows, the SO_REUSEADDR option means that multiple server sockets can
    be bound to the same address (it has nothing to do with TIME_WAIT).
    """
    if os.name != "nt":
        server_socket.setsockopt(
            socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)


def get_arg_utf8(arg_type, value):
    if arg_type == STRING_TYPE:
        return unescape_new_line(value)
    elif arg_type == COMMAND_TYPE:
        return value
    elif arg_type == LONG_TYPE or arg_type == INT_TYPE:
        return int(value)
    elif arg_type == DOUBLE_TYPE:
        return float(value)
    elif arg_type == BYTE_TYPE:
        value = base64.b64decode(value.encode("ascii"))
        return value


def process_line_by_line(socket_instance):
    stats = defaultdict(list)
    try:
        input = socket_instance.makefile("rb")
        print(type(input))

        for i in range(ITERATIONS):
            count = 0
            start = time.time()
            args = []
            while True:
                arg_type = input.readline()[:-1].decode("utf-8")
                if arg_type == END:
                    break
                arg_type = int(arg_type)
                count += 1
                value = input.readline()[:-1].decode("utf-8")
                args.append(get_arg_utf8(arg_type, value))
            socket_instance.sendall(b"1")

            stop = time.time()
            timeSec = stop - start
            stats[count].append(timeSec * 1000)
    finally:
        quiet_close(socket_instance)

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def read_arg(stream):
    arg_type = struct.unpack("!i", stream.read(4))[0]
    if arg_type in LENGTH_TYPES:
        size = struct.unpack("!i", stream.read(4))[0]
        value = stream.read(size)
        if arg_type == BYTE_TYPE:
            return value
        else:
            return value.decode("utf-8")
    elif arg_type == INT_TYPE:
        return struct.unpack("!i", stream.read(4))[0]
    elif arg_type == LONG_TYPE:
        return struct.unpack("!q", stream.read(8))[0]
    elif arg_type == DOUBLE_TYPE:
        return struct.unpack("!d", stream.read(8))[0]
    else:
        raise Exception()


def process_optimized(socket_instance):
    stats = defaultdict(list)
    try:
        input = socket_instance.makefile("rb")

        for i in range(ITERATIONS):
            start = time.time()
            args = []
            count = struct.unpack("!i", input.read(4))[0]
            for i in range(count):
                args.append(read_arg(input))
            socket_instance.sendall(b"1")

            stop = time.time()
            timeSec = stop - start
            stats[count].append(timeSec * 1000)
    finally:
        quiet_close(socket_instance)

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


class ProcessSocket(Thread):

    def __init__(self, socket):
        super(ProcessSocket, self).__init__()
        self.socket = socket

    def run(self):
        # process_line_by_line(self.socket)
        process_optimized(self.socket)


def main():
    address = "127.0.0.1"
    port = 10000
    af_type = socket.getaddrinfo(address, port)[0][0]
    server_socket = socket.socket(af_type, socket.SOCK_STREAM)
    set_reuse_address(server_socket)

    try:
        server_socket.bind((address, port))
        server_socket.listen(5)
        read_list = [server_socket]
        while True:
            readable, writable, errored = select.select(
                read_list, [], [])

            for s in readable:
                socket_instance, _ = server_socket.accept()
                process = ProcessSocket(socket_instance)
                process.start()
    except Exception:
        print_exc()
    finally:
        quiet_close(server_socket)


if __name__ == "__main__":
    main()
