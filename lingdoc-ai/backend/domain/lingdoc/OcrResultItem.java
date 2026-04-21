package com.ruoyi.system.domain.lingdoc;

import java.util.List;

/**
 * OCR 识别结果项 VO
 * 
 * @author lingdoc
 */
public class OcrResultItem
{
    /** 页码（从1开始） */
    private Integer pageNum;

    /** 识别文本 */
    private String text;

    /** 置信度（0-1） */
    private Double confidence;

    /** 边界框坐标 [x1, y1, x2, y2, x3, y3, x4, y4] */
    private List<Double> bbox;

    /** 区域类型：text/table/header/footer */
    private String regionType;

    public Integer getPageNum()
    {
        return pageNum;
    }

    public void setPageNum(Integer pageNum)
    {
        this.pageNum = pageNum;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public Double getConfidence()
    {
        return confidence;
    }

    public void setConfidence(Double confidence)
    {
        this.confidence = confidence;
    }

    public List<Double> getBbox()
    {
        return bbox;
    }

    public void setBbox(List<Double> bbox)
    {
        this.bbox = bbox;
    }

    public String getRegionType()
    {
        return regionType;
    }

    public void setRegionType(String regionType)
    {
        this.regionType = regionType;
    }
}
