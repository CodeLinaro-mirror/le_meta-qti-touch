DESCRIPTION = "QTI Touch drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/\
${LICENSE};md5=801f80980d171dd6425610833a22dbe6"

inherit linux-kernel-base

PR = "r0"

DEPENDS = "rsync-native"

do_configure[depends] += "virtual/kernel:do_shared_workdir"

FILESPATH   =+ "${WORKSPACE}:"
SRC_URI     =  "file://vendor/qcom/opensource/touch-drivers/"
SRC_URI    +=  "file://start_touch_le"
SRC_URI    +=  "file://touch.service"
SRC_URI    +=  "file://touch_load.conf"

S = "${WORKDIR}/vendor/qcom/opensource/touch-drivers"

EXTRA_OEMAKE += "TARGET_SUPPORT=${BASEMACHINE}"

# Disable parallel make
PARALLEL_MAKE = ""

# Disable parallel make
PARALLEL_MAKE = "-j1"

do_compile[lockfiles] = "${TMPDIR}/build_modules.lock"

do_configure() {
	cp -f ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile.am ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile
}

do_compile() {
    cd ${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform  \

    BUILD_CONFIG=msm-kernel/build.config.msm.sun.tuivm \
	KERNEL_KIT=${KERNEL_PREBUILT_PATH} \
    OUT_DIR=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/out/*_tuivm-${KERNEL_VARIANT}defconfig/ \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh

    BUILD_CONFIG=msm-kernel/build.config.msm.sun.tuivm \
    EXT_MODULES=../../vendor/qcom/opensource/touch-drivers \
    ROOTDIR=${WORKSPACE}/ \
    MODULE_MSM_TOUCH=m \
    MODULE_OUT=${WORKDIR}/vendor/qcom/opensource/touch-drivers \
    OUT_DIR=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/out/*_tuivm-${KERNEL_VARIANT}defconfig/ \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh

    BUILD_CONFIG=msm-kernel/build.config.msm.parrot.tuivm \
	KERNEL_KIT=${KERNEL_PREBUILT_PATH} \
    OUT_DIR=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/out/*_tuivm-${KERNEL_VARIANT}defconfig/ \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh

    BUILD_CONFIG=msm-kernel/build.config.msm.parrot.tuivm \
    EXT_MODULES=../../vendor/qcom/opensource/touch-drivers \
    ROOTDIR=${WORKSPACE}/ \
    MODULE_MSM_TOUCH=m \
    MODULE_OUT=${WORKDIR}/vendor/qcom/opensource/touch-drivers \
    OUT_DIR=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/out/*_tuivm-${KERNEL_VARIANT}defconfig/ \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh
}

do_install() {
	install -d ${D}${sysconfdir}/initscripts
	install -d ${D}${sbindir}/initscripts
	install -d ${D}${systemd_unitdir}/system/multi-user.target.wants/
	install -m 755 ${WORKDIR}/start_touch_le ${D}${sbindir}/initscripts
	install -d ${D}/usr/lib/modules/
	install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko -D ${D}${libdir}/modules/goodix_ts.ko
	install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/atmel_mxt_ts.ko -D ${D}${libdir}/modules/atmel_mxt_ts.ko
	install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko -D ${D}${libdir}/modules/qts.ko
	install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko -D ${D}${libdir}/modules/st_fts.ko
	install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko -D ${D}${libdir}/modules/focaltech_fts.ko
    install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/gt9xx-ts.ko -D ${D}${libdir}/modules/gt9xx-ts.ko
	install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
	install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
	ln -sf ${systemd_unitdir}/system/touch.service ${D}${systemd_unitdir}/system/multi-user.target.wants/touch.service
}

FILES:${PN} += "${sysconfdir}/*"
FILES:${PN} += "${sbindir}/initscripts/start_touch_le"
FILES:${PN} += "${systemd_unitdir}/system/touch.service"
FILES:${PN} += "${systemd_unitdir}/system/multi-user.target.wants/touch.service"
FILES:${PN} += "${libdir}/modules/*"
