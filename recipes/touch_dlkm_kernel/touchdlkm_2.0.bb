DESCRIPTION = "QTI Touch drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

inherit linux-kernel-base deploy

PR = "r0"
PV = "2.0+git"

DEPENDS = "rsync-native displaydlkm"

do_configure[depends] += "virtual/kernel:do_shared_workdir"

FILESPATH   =+ "${WORKSPACE}:"

SRC_URI     =  "file://vendor/qcom/opensource/touch-drivers/"
SRC_URI    +=  "file://start_touch_le"
SRC_URI    +=  "file://touch.service"
SRC_URI    +=  "file://touch_load.conf"
SRC_URI    +=  "file://sign-file"
SRC_URI    +=  "file://signing_key.x509"

S = "${WORKDIR}/vendor/qcom/opensource/touch-drivers"

EXTRA_OEMAKE += "TARGET_SUPPORT=${BASEMACHINE}"

PARALLEL_MAKE = "-j1"

LD_PATH = "${@oe.utils.conditional('KERNEL_TOOLS_USES_MUSLC', 'True', "${LD_PATH_MUSLC}", "${LD_PATH_GLIBC}", d)}"

do_compile[lockfiles] = "${TMPDIR}/build_modules.lock"

do_configure() {
    cp -f ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile.am ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile
}

do_compile() {
    cd ${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform && \
    BUILD_CONFIG=${KERNEL_BUILD_CONFIG} \
    EXT_MODULES=../../vendor/qcom/opensource/touch-drivers \
    ROOTDIR=${WORKSPACE}/ \
    ENABLE_DDK_BUILD=${DDK_BUILD} \
    VARIANT=${KERNEL_DEFCONFIG_VARIANT} \
    TARGET_BOARD_PLATFORM=${TARGET_BOARD_PLATFORM} \
    MODULE_MSM_TOUCH=m \
    MODULE_OUT=${WORKDIR}/vendor/qcom/opensource/touch-drivers \
    KERNEL_KIT=${KERNEL_OUT_PATH}/ \
    OUT_DIR=temp_out_dir \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh
}

do_strip_and_sign_modules() {
    install -d ${B}/scripts
    install -d ${B}/certs

    # Copy sign-file and cert from recipe's files/ dir
    install -m 0755 ${WORKDIR}/sign-file ${B}/scripts/sign-file
    install -m 0644 ${WORKDIR}/signing_key.x509 ${B}/certs/signing_key.x509

    export PATH=${STAGING_BINDIR_NATIVE}:$PATH

    # Main modules
    for module in qts.ko st_fts.ko goodix_ts.ko; do
        modfile="${WORKDIR}/vendor/qcom/opensource/touch-drivers/${module}"
        if [ -f "$modfile" ]; then
		${STRIP} --strip-debug "$modfile"
            ${B}/scripts/sign-file sha1 /dev/null ${B}/certs/signing_key.x509 "$modfile"
        fi
    done

    # Only for trustedvm-v4: focaltech_fts.ko
    if ${@bb.utils.contains('MACHINE', 'trustedvm-v4', 'true', 'false', d)}; then
        modfile="${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko"
        if [ -f "$modfile" ]; then
		${STRIP} --strip-debug "$modfile"
            ${B}/scripts/sign-file sha1 /dev/null ${B}/certs/signing_key.x509 "$modfile"
        fi
    fi
}

do_install() {
    install -d ${D}${sysconfdir}/initscripts
    install -m 755 ${WORKDIR}/start_touch_le ${D}${sysconfdir}/initscripts
    install -d ${D}${libdir}/modules/

    # Main modules
    for module in qts.ko st_fts.ko goodix_ts.ko; do
        src="${WORKDIR}/vendor/qcom/opensource/touch-drivers/${module}"
        if [ -f "$src" ]; then
            install -m 0644 "$src" ${D}${libdir}/modules/
            chown 0:0 ${D}${libdir}/modules/$(basename "$src") || true
        fi
    done

    # trustedvm-v4 special module
    if ${@bb.utils.contains('MACHINE', 'trustedvm-v4', 'true', 'false', d)}; then
        src="${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko"
        if [ -f "$src" ]; then
            install -m 0644 "$src" ${D}${libdir}/modules/
            chown 0:0 ${D}${libdir}/modules/focaltech_fts.ko || true
        fi
    fi

    install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
    install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
}

python () {
    bb.build.addtask('do_strip_and_sign_modules', 'do_install', 'do_compile', d)
}

FILES:${PN} += "${sysconfdir}/*"
FILES:${PN} += "/etc/initscripts/start_touch_le"
FILES:${PN} += "${systemd_unitdir}/system/touch.service"
FILES:${PN} += "${libdir}/modules/*"

RM_WORK_EXCLUDE += "${PN}"
