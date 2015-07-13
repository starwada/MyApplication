package com.example.wada.myapplication;

/**
 * Created by Wada on 2015/06/25.
 */
public class Status {
    private String text;
    private String screenName;
    private String profileImageUrl;

    public  String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String getScreenName()
    {
        return screenName;
    }

    public void setScreenName(String screenName)
    {
        this.screenName = screenName;
    }

    public String getProfileImageUrl()
    {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl)
    {
        this.profileImageUrl = profileImageUrl;
    }
}
