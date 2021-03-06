package com.example.q.xmppclient.entity;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import org.jivesoftware.smack.packet.RosterPacket;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.*;
import android.support.annotation.NonNull;

import com.example.q.xmppclient.util.FormatUtil;
import com.example.q.xmppclient.util.StringUtil;

import net.sourceforge.pinyin4j.PinyinHelper;

import org.jivesoftware.smackx.packet.VCard;

import java.util.Comparator;
import java.util.Date;

import static org.jivesoftware.smack.packet.Presence.Mode.available;

/**
 * Created by q on 2017/10/17.
 */

public class User implements Parcelable,Comparable{
    /**
     * 将user保存在intent中时的key
     */
    public static final String userKey = "ichat_user";
    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    private String jid;//jabberid  uid@域名

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;
    private String nickName;//昵称
    private Phone phone;//电话
    private Email email;//邮箱

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    private String country;
    private String sign;
    private String province;
    private String city;
    private Date birthday;//
    private Bitmap icon;//头像
    private VCard vcard;//电子名片

    public RosterPacket.ItemType getItemType() {
        return itemType;
    }

    public void setItemType(RosterPacket.ItemType itemType) {
        this.itemType = itemType;
    }

    private RosterPacket.ItemType itemType;
//    public String getSex() {
//        if(sex)
//            return "man";
//        else
//            return "woman";
//    }
//
//    public void setSex(String sex) {
//        if(sex.equals("man"))
//            this.sex=true;
//        else if(sex.equals("woman"))
//            this.sex=false;
//    }
//
//    private boolean sex;

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap  icon) {
        this.icon = icon;
    }

    public VCard getVCard() {
        return  vcard;
    }
    public void setVCard(VCard vcard) {
        this.vcard = vcard;
    }


    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
    public User()
    {}
    public User(String jid)
    {
        this.jid=jid;
        this.username= StringUtil.getUserNameByJid(jid);
        this.nickName=username;

    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(jid);
        dest.writeString(username);
        dest.writeString(nickName);
//        dest.writeString(from);
//        dest.writeString(status);
//        dest.writeInt(available ? 1 : 0);
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {

        @Override
        public User createFromParcel(Parcel source) {
            User u = new User();
            u.jid = source.readString();
            u.username = source.readString();
            u.nickName=source.readString();
//            u.from = source.readString();
//            u.status = source.readString();
//            u.available = source.readInt() == 1 ? true : false;
            return u;
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }

    };
    public User clone() {
        User user = new User();
//        user.setAvailable(User.this.available);
//        user.setFrom(User.this.from);
//        user.setGroupName(User.this.groupName);
//        user.setImgId(User.this.imgId);
        user.setJid(User.this.jid);
        user.setUsername(User.this.username);
        user.setNickName(User.this.nickName);
//        user.setSize(User.this.size);
//        user.setStatus(User.this.status);
        return user;
    }

    @Override
    public int compareTo(Object o) {
        if(this.nickName==null){
            return 1;
        }
        if (((User)o).nickName==null) {
            return -1;
        }
        String selfStr=this.nickName;
        String otherStr=((User)o).nickName;
        String[] self=selfStr.split("");
        String[] other=otherStr.split("");
        for(int i=0;i<self.length;i++){
            if (i==0){
                selfStr+="&";
            }
            if (self[i].matches("[\\u4E00-\\u9FA5]+")) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(self[i].toCharArray()[0]);
                selfStr+=pinyinArray[0].charAt(0);
            }else if(self[i].matches("[0-9a-zA-Z]")){

                    selfStr+=self[i];
            }
        }
        for(int i=0;i<other.length;i++){
            if (i==0){
                otherStr+="&";
            }
            if (other[i].matches("[\\u4E00-\\u9FA5]+")) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(other[i].toCharArray()[0]);
                otherStr+=pinyinArray[0].charAt(0);
            }else if(other[i].matches("[0-9a-zA-Z]")){
                otherStr+=other[i];
            }
        }
        selfStr=selfStr.split("&")[1];
        selfStr=selfStr.toLowerCase();
        otherStr=otherStr.split("&")[1];
        otherStr=otherStr.toLowerCase();
        return selfStr.compareTo(otherStr);
    }

}
