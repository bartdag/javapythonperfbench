import base64
from collections import defaultdict
from functools import partial
import os
import socket
import struct
from statistics import mean, stdev
import time


END = "e\n"

COMMAND = "a"

REF_STRING = "r15"

SMALL_STRING = "hello"

LONG_STRING = "very long string ééééééééééééé" * 10

EXTRA_LONG_STRING = "very long string \n ééééééééééééé" * 1000

ESCAPE_CHAR = "\\"

INTEGER = -789456123

LONG = 361231231231231231

DOUBLE = 1.0 / 3.0

SMALL_BYTES = os.urandom(64)

BYTES = os.urandom(1024) * 1024 * 64

EXTRA_BYTES = os.urandom(1024) * 1024 * 256

BYTE_TYPE = 0
STRING_TYPE = 1
COMMAND_TYPE = 2
INT_TYPE = 3
LONG_TYPE = 4
DOUBLE_TYPE = 5

BYTE_BYTE_TYPE = bytes((0,))
BYTE_STRING_TYPE = bytes((1,))
BYTE_COMMAND_TYPE = bytes((2,))
BYTE_INT_TYPE = bytes((3,))
BYTE_LONG_TYPE = bytes((4,))
BYTE_DOUBLE_TYPE = bytes((5,))

LENGTH_TYPES = (BYTE_TYPE, STRING_TYPE, COMMAND_TYPE)

ITERATIONS = 50000

WAIT_TIME = 0.250


def get_args(count):
    args = []
    if count == 1:
        args.append((COMMAND_TYPE, COMMAND))
    elif count == 2:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, REF_STRING))
    elif count == 3:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, REF_STRING))
    elif count == 4:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((INT_TYPE, INTEGER))
        args.append((STRING_TYPE, SMALL_STRING))
    elif count == 5:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((DOUBLE_TYPE, DOUBLE))
        args.append((DOUBLE_TYPE, DOUBLE))
    elif count == 6:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((LONG_TYPE, LONG))
        args.append((LONG_TYPE, LONG))
    elif count == 7:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((COMMAND_TYPE, COMMAND))
        args.append((STRING_TYPE, EXTRA_LONG_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
    elif count == 8:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((BYTE_TYPE, SMALL_BYTES))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
    elif count == 9:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((BYTE_TYPE, BYTES))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
    elif count == 10:
        args.append((COMMAND_TYPE, COMMAND))
        args.append((BYTE_TYPE, EXTRA_BYTES))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))
        args.append((STRING_TYPE, SMALL_STRING))

    return args


def encode_utf8(args):
    new_args = []
    for (arg_type, arg) in args:
        if arg_type != BYTE_TYPE:
            value = str(arg).encode("utf-8")
            new_args.append((len(value), arg_type, value))
        else:
            new_args.append((len(arg), arg_type, arg))
    return new_args


def encode_utf8_lines(args):
    new_args = []
    for (arg_type, arg) in args:
        new_arg_type = str(arg_type) + "\n"
        if arg_type != BYTE_TYPE:
            value = str(arg)
            if arg_type == STRING_TYPE:
                value = escape_new_line(value)
            new_args.append(
                new_arg_type + value + "\n")
        else:
            value = base64.b64encode(arg).decode("ascii")
            new_args.append(
                new_arg_type + value + "\n")
    new_args.append(END)
    return new_args


def encode_bytes(args):
    new_args = []
    for (arg_type, arg) in args:
        if arg_type == BYTE_TYPE:
            value = arg
        elif arg_type == INT_TYPE:
            value = struct.pack("!i", arg)
        elif arg_type == LONG_TYPE:
            value = struct.pack("!q", arg)
        elif arg_type == DOUBLE_TYPE:
            value = struct.pack("!d", arg)
        else:
            value = str(arg).encode("utf-8")

        new_args.append((len(value), arg_type, value))
    return new_args


def encode_bytes_total(args, total=False):
    total_size = 0
    new_args = []
    for (arg_type, arg) in args:
        if arg_type == BYTE_TYPE:
            value = arg
        elif arg_type == INT_TYPE:
            value = struct.pack("!i", arg)
        elif arg_type == LONG_TYPE:
            value = struct.pack("!q", arg)
        elif arg_type == DOUBLE_TYPE:
            value = struct.pack("!d", arg)
        else:
            value = str(arg).encode("utf-8")

        size = len(value)
        # Size (int), Arg Type (int), Size of arg
        total_size += 4 + size
        if arg_type in LENGTH_TYPES:
            total_size += 4

        new_args.append((size, arg_type, value))
    if total:
        return new_args, total_size
    else:
        return new_args


def smart_decode(s):
    if isinstance(s, str):
        return s
    elif isinstance(s, bytes):
        # Should never reach this case in Python 3
        return str(s, "utf-8")
    else:
        return str(s)


def escape_new_line(original):
    """Replaces new line characters by a backslash followed by a n.

    Backslashes are also escaped by another backslash.

    :param original: the string to escape

    :rtype: an escaped string
    """
    return smart_decode(original).replace("\\", "\\\\").replace("\r", "\\r").\
        replace("\n", "\\n")


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


def transferLineByLine(count, stats, iterations):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)

    for i in range(iterations):
        start = time.time()
        args = get_args(count)
        args = encode_utf8_lines(args)
        for arg in args:
            sock.sendall(arg.encode("utf-8"))
        receive(sock)
        stop = time.time()
        timeSec = stop - start
        stats[count].append(timeSec * 1000)

    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def transferWithLength(count, stats, iterations, encode_to_bytes=False,
                       type_as_bytes=False):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)

    for i in range(iterations):
        start = time.time()
        args = get_args(count)
        if encode_to_bytes:
            args = encode_bytes(args)
        else:
            args = encode_utf8(args)
        sock.sendall(struct.pack("!i", len(args)))
        for arg in args:
            # # Always send type first
            arg_type = arg[1]
            if type_as_bytes:
                sock.sendall(arg_type)
            else:
                sock.sendall(struct.pack("!i", arg_type))

            # Optionally send length
            if not encode_to_bytes or arg_type in LENGTH_TYPES:
                sock.sendall(struct.pack("!i", arg[0]))

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

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def transferWithLengthOptimized(count, stats, iterations):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)

    for i in range(iterations):
        start = time.time()
        args = get_args(count)
        args = encode_bytes(args)
        sock.sendall(struct.pack("!i", len(args)))
        MAX_SIZE = 32
        payload = bytearray()
        for arg in args:
            # # Always send type first
            arg_type = arg[1]
            value = arg[2]
            if arg_type in LENGTH_TYPES:
                if len(value) > MAX_SIZE:
                    payload.extend(
                        struct.pack("!i", arg_type) +
                        struct.pack("!i", arg[0]))
                    sock.sendall(payload)
                    payload.clear()
                    sock.sendall(value)
                    continue
                else:
                    payload.extend(
                        struct.pack("!i", arg_type) +
                        struct.pack("!i", arg[0]) + value)
            else:
                payload.extend(struct.pack("!i", arg_type) + value)
            if len(payload) > MAX_SIZE:
                sock.sendall(payload)
                payload.clear()

        if payload:
            sock.sendall(payload)
        receive(sock)
        stop = time.time()
        timeSec = stop - start
        stats[count].append(timeSec * 1000)

    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def transferLengthThanArgs(count, stats, iterations):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)

    for i in range(iterations):
        start = time.time()
        args = get_args(count)
        args = encode_bytes(args)
        sock.sendall(struct.pack("!i", len(args)))
        meta = bytearray()
        for arg in args:
            meta.extend(struct.pack("!i", arg[1]))
            meta.extend(struct.pack("!i", arg[0]))
        sock.sendall(meta)

        # Version 2: build in memory then push
        payload = bytearray()

        for arg in args:
            payload.extend(arg[2])
            # Version 1: send one by one
            # sock.sendall(arg[2])
        sock.sendall(payload)
        receive(sock)
        stop = time.time()
        timeSec = stop - start
        stats[count].append(timeSec * 1000)

    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def transferTotalWithLength(count, stats, iterations):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)

    for i in range(iterations):
        start = time.time()
        args = get_args(count)
        args, total = encode_bytes_total(args)
        total += 4

        # 1. sent total of bytes
        sock.sendall(struct.pack("!i", total))

        # 2. sent number of args
        sock.sendall(struct.pack("!i", len(args)))

        for arg in args:
            # Always send type first
            arg_type = arg[1]
            sock.sendall(struct.pack("!i", arg_type))

            # Optionally send length
            if arg_type in LENGTH_TYPES:
                sock.sendall(struct.pack("!i", arg[0]))

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

    print("Average to send {0} args: {1}. Std Dev: {2}".format(
        count, mean(stats[count]), stdev(stats[count])))


def main():
    global COMMAND_TYPE, STRING_TYPE, INT_TYPE, LONG_TYPE, DOUBLE_TYPE,\
        BYTE_TYPE, LENGTH_TYPES
    type_as_bytes = False
    if type_as_bytes:
        COMMAND_TYPE = BYTE_COMMAND_TYPE
        STRING_TYPE = BYTE_STRING_TYPE
        INT_TYPE = BYTE_INT_TYPE
        LONG_TYPE = BYTE_LONG_TYPE
        DOUBLE_TYPE = BYTE_DOUBLE_TYPE
        BYTE_TYPE = BYTE_BYTE_TYPE
        LENGTH_TYPES = (COMMAND_TYPE, STRING_TYPE, BYTE_TYPE)
    # func = transferWithLength
    func = transferWithLengthOptimized
    # func = partial(transferWithLength, encode_to_bytes=True)
    # func = partial(transferWithLength, encode_to_bytes=True,
    # type_as_bytes=type_as_bytes)
    # func = transferLineByLine
    # func = transferTotalWithLength
    # func = transferLengthThanArgs
    for i in range(11):
        stats = defaultdict(list)
        func(i, stats, ITERATIONS)


if __name__ == "__main__":
    main()
