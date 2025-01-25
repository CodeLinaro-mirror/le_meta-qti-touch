SUMMARY = "QTI Touch package groups"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

LICENSE = "BSD-3-Clause"

PROVIDES = "${PACKAGES}"

PACKAGES = ' \
    packagegroup-qti-touch \
    '

RDEPENDS:packagegroup-qti-touch = ' \
    touchdlkm \
    '
