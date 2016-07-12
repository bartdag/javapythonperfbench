import base64
from collections import defaultdict
import os
import socket
from statistics import mean
import time


END = "e\n"

SMALL_STRING = "hello"

LONG_STRING = "very long string ééééééééééééé" * 10

EXTRA_LONG_STRING = "very long string \n ééééééééééééé" * 1000

ESCAPE_CHAR = "\\"

BYTES = os.urandom(1024) * 1024 * 64

EXTRA_BYTES = os.urandom(1024) * 1024 * 256

STRING = 0
BYTE = 1

ITERATIONS = 100

WAIT_TIME = 0.250


def create_message(count):
    if count == 1:
        return END
    elif count == 2:
        return "hello\n" + END
    elif count == 4:
        return "hello\n1111111111111111111111111\n" +\
            escape_new_line(LONG_STRING) + "\n" + END
    elif count == 5:
        return "hello\n1111111111111111111111111\n" +\
            escape_new_line(EXTRA_LONG_STRING) + "\nr150\n" + END
    elif count == 7:
        return "hello\n1111111111111111111111111\n" +\
            escape_new_line(SMALL_STRING) + "\nr150\ntest\n" +\
            base64.b64encode(BYTES).decode("ascii") + "\n" + END
    elif count == 8:
        return "hello\n1111111111111111111111111\n" +\
            escape_new_line(SMALL_STRING) + "\nr150\ntest\n" +\
            base64.b64encode(EXTRA_BYTES).decode("ascii") + "\n" +\
            "testtest\n" + END


def get_args(count):
    args = []
    if count == 1:
        args.append((STRING, END))
    elif count == 2:
        args.append((STRING, "hello"))
        args.append((STRING, END))
    elif count == 4:
        args.append((STRING, "hello"))
        args.append((STRING, "1111111111111111111111111"))
        args.append((STRING, LONG_STRING))
        args.append((STRING, END))
    elif count == 5:
        args.append((STRING, "hello"))
        args.append((STRING, "1111111111111111111111111"))
        args.append((STRING, EXTRA_LONG_STRING))
        args.append((STRING, "r150"))
        args.append((STRING, END))
    elif count == 7:
        args.append((STRING, "hello"))
        args.append((STRING, "1111111111111111111111111"))
        args.append((STRING, SMALL_STRING))
        args.append((STRING, "r150"))
        args.append((STRING, "test"))
        args.append((BYTE, BYTES))
        args.append((STRING, END))
    elif count == 8:
        args.append((STRING, "hello"))
        args.append((STRING, "1111111111111111111111111"))
        args.append((STRING, SMALL_STRING))
        args.append((STRING, "r150"))
        args.append((STRING, "test"))
        args.append((BYTE, EXTRA_BYTES))
        args.append((STRING, "test"))
        args.append((STRING, END))

    new_args = []
    for (arg_type, arg) in args:
        if arg_type == STRING:
            value = arg.encode("utf-8")
            new_args.append((len(value), arg_type, value))
        else:
            new_args.append((len(arg), arg_type, arg))
    return new_args


def escape_new_line(original):
    """Replaces new line characters by a backslash followed by a n.

    Backslashes are also escaped by another backslash.

    :param original: the string to escape

    :rtype: an escaped string
    """
    return smart_decode(original).replace("\\", "\\\\").replace("\r", "\\r").\
        replace("\n", "\\n")


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


def smart_decode(s):
    if isinstance(s, str):
        return s
    elif isinstance(s, bytes):
        # Should never reach this case in Python 3
        return str(s, "utf-8")
    else:
        return str(s)


def send(sock, data, size):
    totalsent = 0
    while totalsent < size:
        sent = sock.send(data[totalsent:])
        if sent == 0:
            raise RuntimeError("socket connection broken")
        totalsent = totalsent + sent


def receive(sock):
    response = sock.recv(1)
    # print(response)
    return response


def get_int(a_byte):
    return int.from_bytes(a_byte, "big", signed=True)


def transfer(count, stats):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)
    start = time.time()
    msg = create_message(count)
    sock.sendall(msg.encode("utf-8"))
    receive(sock)
    stop = time.time()
    timeSec = stop - start
    stats[count].append(timeSec * 1000)
    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass
    # print("Took {0} to send {1} args. Average: {2}".format(
        # timeSec * 1000, count, mean(stats[count])))


def transferBlock(count, stats):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)
    start = time.time()
    msg = create_message(count)
    msgBytes = msg.encode("utf-8")
    size = len(msgBytes)
    sock.sendall(size.to_bytes(4, "big", signed=True))
    sock.sendall(msgBytes)
    receive(sock)
    stop = time.time()
    timeSec = stop - start
    stats[count].append(timeSec * 1000)
    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass
    # print("Took {0} to send {1} args. Average: {2}".format(
        # timeSec * 1000, count, mean(stats[count])))


def transferWithLength(count, stats):
    pass
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)
    start = time.time()
    args = get_args(count)
    sock.sendall(len(args).to_bytes(4, "big", signed=True))
    for arg in args:
        sock.sendall(arg[0].to_bytes(4, "big", signed=True))
        sock.sendall(arg[1].to_bytes(4, "big", signed=True))
        sock.sendall(arg[2])
    receive(sock)
    stop = time.time()
    timeSec = stop - start
    stats[count].append(timeSec * 1000)
    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass


def make_a_run(func, param):
    stats = defaultdict(list)
    for i in range(ITERATIONS):
        func(param, stats)
        time.sleep(WAIT_TIME)
    print("Average to send {0} args: {1}".format(
        param, mean(stats[param])))


def main():
    # func = transfer
    # func = transferBlock
    func = transferWithLength
    make_a_run(func, 1)
    make_a_run(func, 2)
    make_a_run(func, 4)
    make_a_run(func, 5)
    make_a_run(func, 7)
    make_a_run(func, 8)


if __name__ == "__main__":
    main()
