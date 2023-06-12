SUMMARY = "QTI Touch package groups"

PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit packagegroup

LICENSE = "BSD-3-Clause-Clear"

PROVIDES = "${PACKAGES}"

PACKAGES = ' \
    packagegroup-qti-touch \
    '

LE_VERSION_DIFF="${@bb.utils.contains("KERNEL_MODULES_LIST", "modules.list.msm.kalama.le", ":", "_", d)}"

RDEPENDS${LE_VERSION_DIFF}packagegroup-qti-touch = ' \
    ${@bb.utils.contains("KERNEL_MODULES_LIST", "modules.list.msm.kalama.le", "touch-for-linuxdlkm", "touchdlkm", d)} \
    '
