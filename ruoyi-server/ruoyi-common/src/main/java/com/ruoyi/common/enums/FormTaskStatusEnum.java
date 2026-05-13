package com.ruoyi.common.enums;

/**
 * 表格填写任务状态枚举
 *
 * 替代原有的魔法字符串状态码，提高可维护性
 *
 * @author lingdoc
 */
public enum FormTaskStatusEnum
{
    /** 0 - 待上传 */
    PENDING_UPLOAD("0", "待上传"),

    /** 1 - 识别中 */
    EXTRACTING("1", "识别中"),

    /** 2 - 待确认 */
    AWAIT_CONFIRM("2", "待确认"),

    /** 3 - 已生成 */
    COMPLETED("3", "已生成"),

    /** 4 - 失败 */
    FAILED("4", "失败"),

    /** 5 - 排队中（新增：等待 AI 线程池调度） */
    QUEUED("5", "排队中"),

    /** 6 - AI 处理中（新增：已被线程池领取，正在执行） */
    AI_PROCESSING("6", "AI 处理中");

    private final String code;
    private final String desc;

    FormTaskStatusEnum(String code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getCode()
    {
        return code;
    }

    public String getDesc()
    {
        return desc;
    }

    /**
     * 根据状态码获取枚举
     */
    public static FormTaskStatusEnum fromCode(String code)
    {
        for (FormTaskStatusEnum status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为进行中状态（前端轮询时保持查询）
     */
    public boolean isProcessing()
    {
        return this == EXTRACTING || this == QUEUED || this == AI_PROCESSING || this == COMPLETED;
    }

    /**
     * 判断是否已完成或失败（前端轮询可停止）
     */
    public boolean isTerminal()
    {
        return this == AWAIT_CONFIRM || this == FAILED;
    }
}
