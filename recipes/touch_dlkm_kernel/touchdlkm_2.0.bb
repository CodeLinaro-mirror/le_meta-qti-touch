DESCRIPTION = "QTI Touch drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/${LICENSE};md5=801f80980d171dd6425610833a22dbe6"

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

S = "${WORKDIR}/vendor/qcom/opensource/touch-drivers"

EXTRA_OEMAKE += "TARGET_SUPPORT=${BASEMACHINE}"
KP_STRIP_VERSION ?= "${@bb.utils.contains('BASEMACHINE', 'alor', '13.3.0', '11.4.0', d)}"

GCCVER_AVAILABLE := "${@''.join(filter(lambda x: x != '%', '${GCCVERSION}'))}.0"
STRIP_VERSION = "${GCCVER_AVAILABLE}"

# Disable parallel make
PARALLEL_MAKE = ""

# Disable parallel make
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

         # strip debug symbols and sign the module
         ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${KP_STRIP_VERSION}/strip \
              --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko

         ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${KP_STRIP_VERSION}/strip \
              --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko

         ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${KP_STRIP_VERSION}/strip \
              --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

        if ${@bb.utils.contains('BASEMACHINE', 'alor', 'false','true', d)}; then
            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko

            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko
        fi

        if ${@bb.utils.contains_any('MACHINE', 'trustedvm-v4 alor', 'false','true', d)}; then
            ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${KP_STRIP_VERSION}/strip \
                  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko
            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko
        fi
}

do_install() {
      install -d ${D}${sysconfdir}/initscripts
      if ${@bb.utils.contains('BASEMACHINE', 'alor', 'true','false', d)}; then
          install -d ${D}${systemd_unitdir}/system/multi-user.target.wants/
      fi
      install -m 755 ${WORKDIR}/start_touch_le ${D}${sysconfdir}/initscripts
      install -d ${D}/usr/lib/modules/

      cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko ${D}${libdir}/modules/qts.ko
      cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko ${D}${libdir}/modules/st_fts.ko
      cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko ${D}${libdir}/modules/goodix_ts.ko
      chown 0:0 ${D}${libdir}/modules/qts.ko
      chown 0:0 ${D}${libdir}/modules/st_fts.ko
      chown 0:0 ${D}${libdir}/modules/goodix_ts.ko

      if ${@bb.utils.contains_any('MACHINE', 'trustedvm-v4 alor', 'false','true', d)}; then
          cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko ${D}${libdir}/modules/focaltech_fts.ko
          chown 0:0 ${D}${libdir}/modules/focaltech_fts.ko
      fi

      install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
      install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
      if ${@bb.utils.contains_any('BASEMACHINE', 'alor', 'true','false', d)}; then
          ln -sf ${systemd_unitdir}/system/touch.service ${D}${systemd_unitdir}/system/multi-user.target.wants/touch.service
      fi
}

python () {
    bb.build.addtask('do_strip_and_sign_modules', 'do_install', 'do_compile', d)
}


FILES:${PN} += "${sysconfdir}/*"
FILES:${PN} += "/etc/initscripts/start_touch_le"
FILES:${PN} += "${systemd_unitdir}/system/touch.service"
FILES:${PN} += "${systemd_unitdir}/system/multi-user.target.wants/touch.service"
FILES:${PN} += "${libdir}/modules/*"

RM_WORK_EXCLUDE += "${PN}"
