DESCRIPTION = "QTI Touch drivers"
LICENSE = "GPL-2.0-only"
LICENSE:dunfell = "GPL-2.0"
LICENSE:kirkstone = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/\
${LICENSE};md5=801f80980d171dd6425610833a22dbe6"

inherit linux-kernel-base

PR = "r0"

DEPENDS = "rsync-native displaydlkm"

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
    cd ${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform && \

    BUILD_CONFIG=${KERNEL_BUILD_CONFIG} \
    EXT_MODULES=../../vendor/qcom/opensource/touch-drivers \
    ROOTDIR=${WORKSPACE}/ \
    MODULE_MSM_TOUCH=m \
    MODULE_OUT=${WORKDIR}/vendor/qcom/opensource/touch-drivers \
    KERNEL_KIT=${KERNEL_OUT_PATH}/ \
    OUT_DIR=temp_out_dir \
    KERNEL_UAPI_HEADERS_DIR=${STAGING_KERNEL_BUILDDIR} \
    ./build/build_module.sh
}

do_install() {
	if ${@bb.utils.contains('BASEMACHINE', 'kalama', 'true', 'false', d)}; then
		install -d ${D}${sysconfdir}/initscripts
		install -d ${D}${systemd_unitdir}/system/multi-user.target.wants/
		install -m 755 ${WORKDIR}/start_touch_le ${D}${sysconfdir}/initscripts
		install -d ${D}/usr/lib/modules/
		install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko -D ${D}${libdir}/modules/${KERNEL_VERSION}
		install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko -D ${D}${libdir}/modules/${KERNEL_VERSION}
		install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/atmel_mxt_ts.ko -D ${D}${libdir}/modules/${KERNEL_VERSION}
		install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
		install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
		ln -sf ${systemd_unitdir}/system/touch.service ${D}${systemd_unitdir}/system/multi-user.target.wants/touch.service
	else
		GCC_VERSION_TOUCH=$(aarch64-oe-linux-gcc --version | head -n 1 | awk '{print $3}')

		install -d ${D}${sysconfdir}/initscripts
		install -d ${D}${systemd_unitdir}/system/multi-user.target.wants/
		install -m 755 ${WORKDIR}/start_touch_le ${D}${sysconfdir}/initscripts
		install -d ${D}/usr/lib/modules/

			# strip debug symbols and sign the module
		${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${GCC_VERSION_TOUCH}/strip \
				  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko
		${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${GCC_VERSION_TOUCH}/strip \
				  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko

			LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \
			${KERNEL_PREBUILT_PATH}/msm-kernel/scripts/sign-file sha1 ${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.pem \
			${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko
			LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \
			${KERNEL_PREBUILT_PATH}/msm-kernel/scripts/sign-file sha1 ${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.pem \
			${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko

		install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/qts.ko -D ${D}${libdir}/modules/qts.ko
		install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/focaltech_fts.ko -D ${D}${libdir}/modules/focaltech_fts.ko

			if ${@bb.utils.contains('BASEMACHINE', 'qcm2290-mtp', 'true', 'false', d)}; then
				${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${GCC_VERSION_TOUCH}/strip \
					  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm_ts.ko
				${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${GCC_VERSION_TOUCH}/strip \
					  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/nt36xxx-i2c.ko

				LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \
				${KERNEL_PREBUILT_PATH}/msm-kernel/scripts/sign-file sha1 ${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.pem \
				${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm_ts.ko
				LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \
				${KERNEL_PREBUILT_PATH}/msm-kernel/scripts/sign-file sha1 ${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.pem \
				${KERNEL_PREBUILT_PATH}/msm-kernel/certs/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/nt36xxx-i2c.ko
				LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \

				install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/synaptics_tcm_ts.ko -D ${D}${libdir}/modules/synaptics_tcm_ts.ko
				install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/nt36xxx-i2c.ko -D ${D}${libdir}/modules/nt36xxx-i2c.ko
			else
				# strip debug symbols and sign the module
				${STAGING_DIR_NATIVE}/usr/libexec/aarch64-oe-linux/gcc/aarch64-oe-linux/${GCC_VERSION_TOUCH}/strip \
					  --strip-debug ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

				LD_LIBRARY_PATH=${WORKSPACE}/kernel-${PREFERRED_VERSION_linux-msm}/kernel_platform/prebuilts/kernel-build-tools/linux-x86/lib64/ \
				${KERNEL_PREBUILT_PATH}/../msm-kernel/scripts/sign-file sha1 ${KERNEL_PREBUILT_PATH}/../msm-kernel/certs/signing_key.pem \
				${KERNEL_PREBUILT_PATH}/../msm-kernel/certs/signing_key.x509 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko

			install -m 0755 ${WORKDIR}/vendor/qcom/opensource/touch-drivers/goodix_ts.ko -D ${D}${libdir}/modules/goodix_ts.ko
			fi

		install -m 0644 ${WORKDIR}/touch.service -D ${D}${systemd_unitdir}/system/touch.service
		install -m 0755 ${WORKDIR}/touch_load.conf -D ${D}${sysconfdir}/modules-load.d/touch_load.conf
		ln -sf ${systemd_unitdir}/system/touch.service ${D}${systemd_unitdir}/system/multi-user.target.wants/touch.service
	fi
}

FILES:${PN} += "${sysconfdir}/*"
FILES:${PN} += "/etc/initscripts/start_touch_le"
FILES:${PN} += "${systemd_unitdir}/system/touch.service"
FILES:${PN} += "${systemd_unitdir}/system/multi-user.target.wants/touch.service"
FILES:${PN} += "${libdir}/modules/*"
