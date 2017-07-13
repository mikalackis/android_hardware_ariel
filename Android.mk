LOCAL_PATH := $(call my-dir)

ifeq ($(BOARD_USES_ARIEL_HARDWARE),true)
    $(info ABRAKADABRAAAAAAAA)
    $(info $(BOARD_USES_ARIEL_HARDWARE))
    include $(call all-makefiles-under,$(LOCAL_PATH))
endif
