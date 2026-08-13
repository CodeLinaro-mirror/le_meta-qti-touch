# Built with Kbuild/Makefile (not Bazel)
DESCRIPTION = "QTI Touch drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/${LICENSE};md5=801f80980d171dd6425610833a22dbe6"

inherit linux-kernel-base deploy module

PR = "r0"

COMPATIBLE_MACHINE = "pebble"
DEFAULT_PREFERENCE = "-1"

DEPENDS += " displaydlkm virtual/kernel-toolchain-native linux-msm-headers"

do_configure[depends] += "virtual/kernel:do_shared_workdir"

FILESPATH   =+ "${WORKSPACE}:"
SRC_URI     =  "file://vendor/qcom/opensource/touch-drivers/"
SRC_URI    +=  "file://start_touch_le"
SRC_URI    +=  "file://touch.service"
SRC_URI    +=  "file://touch_load.conf"

S = "${WORKDIR}/vendor/qcom/opensource/touch-drivers"

GCCVER_AVAILABLE := "${@''.join(filter(lambda x: x != '%', '${GCCVERSION}'))}.0"
STRIP_VERSION = "${GCCVER_AVAILABLE}"

LD_PATH = "${@oe.utils.conditional('KERNEL_TOOLS_USES_MUSLC', 'True', "${LD_PATH_MUSLC}", "${LD_PATH_GLIBC}", d)}"

# ============ Build parameters ============
EXTRA_OEMAKE:append = " M=${S}"
EXTRA_OEMAKE:append = " TARGET_SUPPORT=${BASEMACHINE}"
EXTRA_OEMAKE:append = " KCPPFLAGS='-I${STAGING_INCDIR} -I${STAGING_INCDIR}/soc-repo -I${STAGING_INCDIR}/soc-repo/uapi'"
EXTRA_OEMAKE:append = " ROOTDIR=${WORKSPACE}/"
EXTRA_OEMAKE:append = " MODULE_MSM_TOUCH=m"
EXTRA_OEMAKE:append = ' KCFLAGS="-Wno-error=missing-prototypes"'

# ============ Toolchain configuration ============
KERNEL_CC = "${STAGING_BINDIR_NATIVE}/clang/bin/clang \
             -target ${TARGET_ARCH}${TARGET_VENDOR}-${TARGET_OS}"
KERNEL_LD = "${STAGING_BINDIR_NATIVE}/clang/bin/ld.lld"
KERNEL_AR = "${STAGING_BINDIR_NATIVE}/clang/bin/llvm-ar"
KERNEL_OBJCOPY = "${STAGING_BINDIR_NATIVE}/clang/bin/llvm-objcopy"
KERNEL_STRIP = "${STAGING_BINDIR_NATIVE}/clang/bin/llvm-strip"
MAKE_TARGETS = "modules"

do_configure() {
        cp -f ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile.am ${WORKSPACE}/vendor/qcom/opensource/touch-drivers/Makefile
}

do_strip_and_sign_modules() {

    if ${@bb.utils.contains_any('BASEMACHINE', 'alor pebble', 'false','true', d)}; then
         # strip debug symbols and sign the module
         ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${STRIP_VERSION}/strip \
              --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko

        if ${@bb.utils.contains_any('MACHINE', 'trustedvm-malabar', 'false','true', d)}; then
            ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${STRIP_VERSION}/strip \
               --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko
        fi

         ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${STRIP_VERSION}/strip \
              --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

            if ${@bb.utils.contains_any('MACHINE', 'trustedvm-malabar', 'false','true', d)}; then
                LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
                ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko
            fi

            if ${@bb.utils.contains_any('BASEMACHINE', 'trustedvm-v5', 'true','false', d)}; then
               ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${STRIP_VERSION}/strip \
                --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm2_ts.ko
               LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
	           ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm2_ts.ko
	        fi

            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko
    fi

        if ${@bb.utils.contains_any('MACHINE', 'trustedvm-v4 alor pebble', 'false','true', d)}; then
            ${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${STRIP_VERSION}/strip \
                  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko
            LD_LIBRARY_PATH=${LD_PATH} ${KERNEL_PREBUILT_PATH}/dist/sign-file sha1 ${KERNEL_PREBUILT_PATH}/dist/signing_key.pem \
            ${KERNEL_PREBUILT_PATH}/dist/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko
        fi
}

do_install() {
      install -d ${D}${sbindir}/initscripts
      if ${@bb.utils.contains_any('BASEMACHINE', 'alor pebble', 'true','false', d)}; then
          install -d ${D}${systemd_unitdir}/system/multi-user.target.wants/
      fi
      install -m 755 ${WORKDIR}/start_touch_le ${D}${sbindir}/initscripts
      install -d ${D}/usr/lib/modules/

      cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko ${D}${libdir}/modules/qts.ko

      if ${@bb.utils.contains_any('MACHINE', 'trustedvm-malabar', 'false','true', d)}; then
           cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/st_fts.ko ${D}${libdir}/modules/st_fts.ko
           chown 0:0 ${D}${libdir}/modules/st_fts.ko
      fi

      cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko ${D}${libdir}/modules/goodix_ts.ko
      chown 0:0 ${D}${libdir}/modules/qts.ko
      chown 0:0 ${D}${libdir}/modules/goodix_ts.ko

      if ${@bb.utils.contains_any('BASEMACHINE', 'trustedvm-v5 pebble', 'true','false', d)}; then
          cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm2_ts.ko ${D}${libdir}/modules/synaptics_tcm2_ts.ko
          chown 0:0 ${D}${libdir}/modules/synaptics_tcm2_ts.ko
      fi

      if ${@bb.utils.contains_any('MACHINE', 'trustedvm-v4 alor pebble', 'false','true', d)}; then
          cp -rp ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko ${D}${libdir}/modules/focaltech_fts.ko
          chown 0:0 ${D}${libdir}/modules/focaltech_fts.ko
      fi

      install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
      install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
      if ${@bb.utils.contains_any('BASEMACHINE', 'alor pebble', 'true','false', d)}; then
          ln -sf ${systemd_unitdir}/system/touch.service ${D}${systemd_unitdir}/system/multi-user.target.wants/touch.service
      fi
}

python () {
    bb.build.addtask('do_strip_and_sign_modules', 'do_install', 'do_compile', d)
}


FILES:${PN} += "${sysconfdir}/*"
FILES:${PN} += "${sbindir}/initscripts/start_touch_le"
FILES:${PN} += "${systemd_unitdir}/system/touch.service"
FILES:${PN} += "${systemd_unitdir}/system/multi-user.target.wants/touch.service"

RM_WORK_EXCLUDE += "${PN}"
