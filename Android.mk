LOCAL_PATH := $(call my-dir)

ifeq ($(BOARD_USES_ARIEL_HARDWARE),true)
    include $(call all-makefiles-under,$(LOCAL_PATH))
endif
