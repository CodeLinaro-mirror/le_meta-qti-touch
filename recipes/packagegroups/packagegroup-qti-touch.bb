SUMMARY = "QTI Touch package groups"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

LICENSE = "BSD-3-Clause-Clear"

PROVIDES = "${PACKAGES}"

PACKAGES = ' \
    packagegroup-qti-touch \
    '

LE_VERSION_DIFF ="${@bb.utils.contains("DISTRO_CODENAME", "kirkstone", ":", "_", d)}"

RDEPENDS${LE_VERSION_DIFF}packagegroup-qti-touch = ' \
    ${@bb.utils.contains("DISTRO_CODENAME", "kirkstone", "touchdlkm", "touch-for-linuxdlkm", d)} \
    '
