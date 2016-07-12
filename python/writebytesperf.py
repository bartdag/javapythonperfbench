import base64
from collections import defaultdict
import os
import socket
from statistics import mean
import time


SMALL = 4

MEDIUM = 1024

LARGE = 1024 * 512

EXTRA_LARGE = 1024 * 1024 * 64

EXTRA_EXTRA_LARGE = 1024 * 1024 * 1024


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


def transfer(size, stats, use_base64=False):
    if size <= 1024:
        to_transfer = os.urandom(size)
    else:
        # urandom is very slow for very large number
        to_transfer = os.urandom(1024) * (size // 1024)
    # print("{0} to {1}".format(
        # get_int(to_transfer[0:1]), get_int(to_transfer[-1:])))
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('127.0.0.1', 10000)
    sock.connect(server_address)
    start = time.time()
    if use_base64:
        to_transfer = base64.b64encode(to_transfer)
        size = len(to_transfer)
        # print(to_transfer)
    # print("{0} to {1}".format(
        # get_int(to_transfer[0:1]), get_int(to_transfer[-1:])))
    send(sock, size.to_bytes(4, "big", signed=True), 4)
    send(sock, to_transfer, size)
    receive(sock)
    stop = time.time()
    timeSec = stop - start
    stats[size].append(timeSec * 1000)
    try:
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()
    except Exception:
        pass
    print("Took {0} to send {1} bytes. Average: {2}".format(
        timeSec * 1000, size, mean(stats[size])))


def main():
    base64 = True
    stats = defaultdict(list)
    for i in range(50):
        transfer(SMALL, stats, base64)
        time.sleep(1)
        transfer(MEDIUM, stats, base64)
        time.sleep(1)
        transfer(LARGE, stats, base64)
        time.sleep(1)
        transfer(EXTRA_LARGE, stats, base64)
        time.sleep(1)
        transfer(EXTRA_EXTRA_LARGE, stats, base64)


if __name__ == "__main__":
    main()
