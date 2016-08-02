from collections import defaultdict
import os
import select
import socket
from statistics import mean, stdev
import time
from threading import Thread
from traceback import print_exc


END = "e"


BYTE_TYPE = 0
STRING_TYPE = 1
COMMAND_TYPE = 2
INT_TYPE = 3
LONG_TYPE = 4
DOUBLE_TYPE = 5

ITERATIONS = 10


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


def process_line_by_line(socket_instance):
    stats = defaultdict(list)
    try:
        input = socket_instance.makefile("rb")

        for i in range(ITERATIONS):
            count = 0
            start = time.time()
            while True:
                arg_type = input.readline()[:-1].decode("utf-8")
                if arg_type == END:
                    break
                value = input.readline()[:-1].decode("utf-8")
                if len(value) < 100:
                    print(value)
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
        # input = self.socket.makefile("rb")
        process_line_by_line(self.socket)


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
