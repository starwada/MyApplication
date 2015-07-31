package com.example.wada.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 * そらまめデータ
 * Intentで渡せるようにParcelableを使用
 * Created by Wada on 2015/07/03.
 */
public class Soramame implements Parcelable{

    // そらまめの測定局データ
    public  class SoramameStation{
        private int m_nCode;                // 測定局コード
        private String m_strName;       // 測定局名称
        private String m_strAddress;    // 住所

        public SoramameStation(int nCode, String strName, String strAddress)
        {
            setCode(nCode);
            setName(strName);
            setAddress(strAddress);
        }
        public void setCode(int nCode)
        {
            m_nCode = nCode;
        }
        public void setName(String strName)
        {
            m_strName = strName;
        }
        public void setAddress(String strAddress)
        {
            m_strAddress = strAddress;
        }

        public int getCode()
        {
            return m_nCode;
        }
        public String getName()
        {
            return m_strName;
        }
        public String getAddress()
        {
            return m_strAddress;
        }
        public String getString()
        {
//            return String.format("%d %s:%s", m_nCode, m_strName, m_strAddress);
            return String.format("%s:%s", m_strName, m_strAddress);
        }
    }

    // そらまめの測定データクラス
    public class SoramameData {
        private GregorianCalendar m_dDate;       // 測定日時 UTCのみのようだ
        private Integer m_nPM25;    // PM2.5測定値 未計測は-100を設定

        SoramameData(String strYear, String strMonth, String strDay, String strHour, String strValue)
        {
            m_dDate = new GregorianCalendar(Integer.valueOf(strYear), Integer.valueOf(strMonth),
                    Integer.valueOf(strDay), Integer.valueOf(strHour), 0);
            // 未計測の場合、"-"が出力される。他のパターンもあった。
//            if( strValue.codePointAt(0) == 12288 || strValue.equalsIgnoreCase("-") ){ m_nPM25 = -100 ; }
//            else{ m_nPM25 = Integer.valueOf(strValue); }
            try{
                m_nPM25 = Integer.parseInt(strValue);
            }
            catch(NumberFormatException e){
                e.getMessage();
                m_nPM25 = -100;
            }
        }

        SoramameData(GregorianCalendar date, Integer nPM25){
            m_dDate = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.HOUR_OF_DAY), 0, 0);
            m_nPM25 = nPM25;
        }

        public GregorianCalendar getDate()
        {
            return m_dDate;
        }
        public String getCalendarString(){
            return String.format("%s/%s/%s %s時",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH), m_dDate.get(Calendar.DAY_OF_MONTH), m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public String getDateString(){
            return String.format("%s/%s/%s",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH), m_dDate.get(Calendar.DAY_OF_MONTH));
        }
        public String getHourString(){
            return String.format("%d時", m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public  Integer getPM25()
        {
            return (m_nPM25 < 0 ? 0 : m_nPM25);
        }
        public String getPM25String(){ return String.format("%s",(m_nPM25 < 0 ? "未計測" : m_nPM25.toString()));}

        public void setPM25(Integer pm25)
        {
            m_nPM25 = pm25;
        }

        public String Format()
        {
            return String.format("%s:%s", getDateString(), getPM25String()) ;
        }
    }

    private SoramameStation m_Station;
    private ArrayList< SoramameData > m_aData;  // 測定データ

    @Override
    public int describeContents(){
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(m_Station.getCode());
        out.writeString(m_Station.getName());
        out.writeString(m_Station.getAddress());
    }

    public static final Parcelable.Creator<Soramame> CREATOR
            = new Parcelable.Creator<Soramame>() {
        public Soramame createFromParcel(Parcel in) {
            return new Soramame(in);
        }

        public Soramame[] newArray(int size) {
            return new Soramame[size];
        }
    };
    public Soramame(){
        super();
    }

    private Soramame(Parcel in) {
        m_Station = new SoramameStation(in.readInt(), in.readString(), in.readString());
        m_aData  = null;
    }

    Soramame(int nCode, String strName, String strAddress)
    {
        m_Station = new SoramameStation(nCode, strName, strAddress);
        m_aData = null ;
    }

    public Integer getMstCode()
    {
        return m_Station.getCode();
    }
    public String getMstName()
    {
        return m_Station.getName();
    }
    public String getAddress()
    {
        return m_Station.getAddress();
    }

    public void setData(String strYear, String strMonth, String strDay, String strHour, String strValue)
    {
        SoramameData data = new SoramameData(strYear, strMonth, strDay, strHour, strValue);
        addData(data);
    }

    public void setData(SoramameData orig){
        SoramameData data = new SoramameData(orig.getDate(), orig.getPM25());
        addData(data);
    }

    public void clearData(){
        if( m_aData != null){
            m_aData.clear();
        }
    }

    private void addData(SoramameData data){
        if( m_aData == null){
            m_aData = new ArrayList<SoramameData>();
        }
        m_aData.add(data);
    }

    public String getStationInfo()
    {
        return m_Station.getString();
    }
    public String getData(int nIndex)
    {
        if( getSize() < 1){ return ""; }

        return m_aData.get(nIndex).Format() ;
    }

    public int getSize()
    {
        return m_aData.size();
    }

    public ArrayList<SoramameData> getData()
    {
        return m_aData;
    }
}
