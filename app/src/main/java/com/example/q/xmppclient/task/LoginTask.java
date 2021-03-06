package com.example.q.xmppclient.task;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.q.xmppclient.R;
import com.example.q.xmppclient.activity.LoginActivity;
import com.example.q.xmppclient.activity.MainActivity;
import com.example.q.xmppclient.activity.IActivity;
import com.example.q.xmppclient.activity.RegisterActivity;
import com.example.q.xmppclient.common.Constant;
import com.example.q.xmppclient.db.DataBaseHelper;
import com.example.q.xmppclient.entity.ChatMessage;
import com.example.q.xmppclient.entity.Notice;
import com.example.q.xmppclient.entity.User;
import com.example.q.xmppclient.manager.LoginConfig;
import com.example.q.xmppclient.manager.MessageManager;
import com.example.q.xmppclient.manager.NoticeManager;
import com.example.q.xmppclient.manager.XmppConnectionManager;
import com.example.q.xmppclient.util.AppUtil;
import com.example.q.xmppclient.util.DateUtil;
import com.example.q.xmppclient.util.FormatUtil;
import com.example.q.xmppclient.util.StringUtil;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.bookmark.Bookmarks;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.VCard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Created by q on 2017/10/26.
 */
public class LoginTask extends AsyncTask<String, Integer, Integer> {
    public static String TAG="LoginTask";
    private ProgressDialog progressDialog;
    private Context context;
    private LoginConfig loginConfig;
    private IActivity activityTool;
    private  XmppConnectionManager xmppConnectionManager;
    private XMPPConnection xmppConnection;
    private String imageDir ;
    private String fileName ;
    private VCard vcard;

    public LoginTask(IActivity activityTool, LoginConfig loginConfig) {
        this.activityTool = activityTool;
        this.loginConfig = loginConfig;
        this.progressDialog = activityTool.getProgressDialog();
        this.context = activityTool.getContext();
        xmppConnectionManager=XmppConnectionManager.getInstance();
        imageDir = Environment.getExternalStorageDirectory()
                .getAbsolutePath() +
                activityTool.getContext().getResources().getString(R.string.img_dir)+"/";
        fileName = "avatar_" +StringUtil.getJidByName
                (loginConfig.getUsername(),loginConfig.getServerName())+".png";
    }

    @Override
    protected void onPreExecute() {
        if(activityTool.getClass().equals(LoginActivity.class)) {
            progressDialog.setMessage("正在登录");
            progressDialog.show();
        }
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(String... params) {
        return login();
    }

    @Override
    protected void onPostExecute(Integer result) {
        if(activityTool.getClass().equals(LoginActivity.class)) {
        progressDialog.dismiss();
        }
        Intent intent=new Intent(Constant.LOGIN_FAILED);
        switch (result) {
            case Constant.LOGIN_SUCCESS:
                Toast.makeText(context, Constant.LOGIN_SUCCESS_MESSAGE, Toast.LENGTH_SHORT).show();
                intent=null;
                intent=new Intent(Constant.REFRESH_PERSONALINFO);
                context.sendBroadcast(intent);
                break;
            case Constant.LOGIN_ERROR:
                Toast.makeText(context,Constant.LOGIN_ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
                context.sendBroadcast(intent);
                break;
            case Constant.SERVER_UNAVAILABLE:
                Toast.makeText(context,Constant.SERVER_UNAVAILABLE_MESSAGE, Toast.LENGTH_SHORT).show();
                context.sendBroadcast(intent);
                break;
            case Constant.USERNAME_PWD_ERROR:
                Toast.makeText(context,Constant.USERNAME_PWD_ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
                context.sendBroadcast(intent);
                break;
            case Constant.UNKNOWN:
                Toast.makeText(context,Constant.UNKNOWN_MESSAGE, Toast.LENGTH_SHORT).show();
                context.sendBroadcast(intent);
                break;
            default:
                break;
        }
        super.onPostExecute(result);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    private Integer login() {
        String username = loginConfig.getUsername();
        String password = loginConfig.getPassword();
        try {
            if (xmppConnection==null) {
                xmppConnection = XmppConnectionManager.getInstance().getConnection();
            }
            if(!xmppConnection.isConnected()) {
                ExecutorService executor= Executors.newSingleThreadExecutor();
                FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                         xmppConnection.connect();
                        return true;
                    }
                });
                executor.execute(future);
                try {
                    Boolean result = future.get(10000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                    return Constant.SERVER_UNAVAILABLE;
                }finally{
                    future.cancel(true);
                }
            }
            xmppConnection.login(username, password);
            //是新用户则给用户创建VCard
            if(loginConfig.isNewUser()) {
                createNewVCardForNewUser(loginConfig);
            }
            loginConfig.setUsername(username);
            loginConfig.setPassword(password);
            loginConfig.setOnline(true);
            //todo 获取离线消息
            getOfflineMsg(context,loginConfig.getUsername());
            Intent intent=new Intent(Constant.GET_OFFLINEMSG);
            context.sendBroadcast(intent);
            //todo 获取离线消息
            //告诉服务器上线了
            Presence presence = new Presence(Presence.Type.available);
            xmppConnection.sendPacket(presence);
            if (activityTool.getClass().equals(LoginActivity.class)) {
                loginByLoginActivity();
            }else if (activityTool.getClass().equals(RegisterActivity.class)) {
                loginByRegisterActivity();
            }else if (activityTool.getClass().equals(MainActivity.class)){
                loginByMainActivity();
            }
        } catch (Exception exc) {
            int code;

            if (exc instanceof XMPPException) {
                XMPPException xe = (XMPPException) exc;
                final XMPPError xmppError = xe.getXMPPError();
                if(xmppError!=null)
                {
                    code = xmppError.getCode();
                    if(code==502)
                    {
                        return Constant.SERVER_UNAVAILABLE;
                    } else if (code==401||code==403)
                    {
                        return Constant.USERNAME_PWD_ERROR;
                    }else
                        return Constant.UNKNOWN;
                }
            }
            if(exc==null){
                return Constant.USERNAME_PWD_ERROR;
            }
            return Constant.LOGIN_ERROR;
        }
        return Constant.LOGIN_SUCCESS;
    }
    /**
     * 新用户VCard新建
     */
    private void createNewVCardForNewUser(LoginConfig loginconfig)
    {
        VCard vCard=new VCard();
        vCard.setNickName(loginconfig.getNickname());
        vCard.setAvatar(AppUtil.getDefaultAvatar(context,loginconfig));//从本地获取默认头像
        vCard.setAddressFieldHome(Constant.COUNTRY,"");
        vCard.setAddressFieldHome(Constant.PROVINCE,"");
        vCard.setAddressFieldHome(Constant.CITY,"");
        vCard.setAddressFieldHome(Constant.SIGN,"");
        try {
            //防止保存失败，从而导致sp里还是原来的NewUser，保存成功修改为false
            activityTool.saveLoginConfig(loginconfig);
            vCard.save(xmppConnection);
            loginConfig.setNewUser(false);
            loginConfig.setAvatar(imageDir+fileName);
            loginConfig.setCountry(vCard.getAddressFieldHome(Constant.COUNTRY));
            loginConfig.setProvince(vCard.getAddressFieldHome(Constant.PROVINCE));
            loginConfig.setCity(vCard.getAddressFieldHome(Constant.CITY));
            loginConfig.setSign(vCard.getAddressFieldHome(Constant.SIGN));
            activityTool.saveLoginConfig(loginconfig);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }
    /**
     * loginByLoginActivity
     */
    private void loginByLoginActivity()
    {
        vcard=new VCard();
        try {
            //todo 要改
            if (XmppConnectionManager.getInstance().getConnection().isConnected()) {
                vcard.load(XmppConnectionManager.getInstance().getConnection());
            }else
            {
                XmppConnectionManager.getInstance().getConnection().connect();
                vcard.load(XmppConnectionManager.getInstance().getConnection());
            }
        }catch (XMPPException e)
        {
            Toast.makeText(context,"VCard加载失败",Toast.LENGTH_SHORT).show();//test 加载失败
        }
        Intent intent = new Intent(context, MainActivity.class);
        if (loginConfig.isFirstStart()) {
            //todo 首次登录动画界面
            //SetVcardTask setVcardTask=new SetVcardTask(loginConfig.getNickname());
            //setVcardTask.execute();
            DataBaseHelper dbhelper = new DataBaseHelper(context, loginConfig.getUsername(), null,3);
            dbhelper.getWritableDatabase();
            loginConfig.setFirstStart(false);
        }
        //todo 保存当前登录用户信息到sharepreference（头像路径、地区、签名）
        loginConfig.setAvatar(imageDir+fileName);
        loginConfig.setNickname(vcard.getNickName());
        loginConfig.setProvince(vcard.getAddressFieldHome("province"));
        loginConfig.setCity(vcard.getAddressFieldHome("city"));
        loginConfig.setSign(vcard.getAddressFieldHome("sign"));
        loginConfig.setCountry(vcard.getAddressFieldHome("country"));
        activityTool.saveLoginConfig(loginConfig);
        saveCurrentUser();
        //缓存头像
        AppUtil.cachedAvatarImage(context,FormatUtil.Bytes2Bitmap(vcard.getAvatar()),
                StringUtil.getJidByName(loginConfig.getUsername(),loginConfig.getServerName()));
        activityTool.startService();
        context.startActivity(intent);
    }
    private  void  saveCurrentUser(){
        MainActivity.currentUser=new User();
        MainActivity.currentUser.setJid(StringUtil.getJidByName(loginConfig.getUsername(),
                loginConfig.getServerName()));
        MainActivity.currentUser.setIcon(FormatUtil.Bytes2Bitmap(vcard.getAvatar()));
        MainActivity.currentUser.setProvince(loginConfig.getProvince());
        MainActivity.currentUser.setCity(loginConfig.getCity());
        MainActivity.currentUser.setCountry(loginConfig.getCountry());
        MainActivity.currentUser.setSign(loginConfig.getSign());
        MainActivity.currentUser.setUsername(loginConfig.getUsername());
        MainActivity.currentUser.setNickName(loginConfig.getNickname());
    }
    /**
     * loginByRegisterActivity
     */
    private void loginByRegisterActivity() {
        //todo 这里不加载vcard，缓存头像直接缓存默认头像到本地。
        vcard=new VCard();
        try {
            //todo 要改
            if (XmppConnectionManager.getInstance().getConnection().isConnected()) {
                vcard.load(XmppConnectionManager.getInstance().getConnection(),
                        StringUtil.getJidByName(loginConfig.getUsername(),
                                loginConfig.getServerName()));
            }else
            {
                XmppConnectionManager.getInstance().getConnection().connect();
                vcard.load(XmppConnectionManager.getInstance().getConnection(),
                        StringUtil.getJidByName(loginConfig.getUsername(),
                                loginConfig.getServerName()));
            }
        }catch (XMPPException e)
        {
            Toast.makeText(context,"VCard加载失败",Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(context, MainActivity.class);
        if (loginConfig.isFirstStart()) {
            //todo 首次登录动画界面  这里可以删掉（待考虑）
            //SetVcardTask setVcardTask=new SetVcardTask(loginConfig.getNickname());
            //setVcardTask.execute();
            DataBaseHelper dbhelper = new DataBaseHelper(context, loginConfig.getUsername(), null,3);
            dbhelper.getWritableDatabase();
            loginConfig.setFirstStart(false);
            activityTool.saveLoginConfig(loginConfig);
        }
        saveCurrentUser();
        activityTool.startService();
        //缓存头像
        AppUtil.cachedAvatarImage(context,FormatUtil.Bytes2Bitmap(vcard.getAvatar()),
                StringUtil.getJidByName(loginConfig.getUsername(),loginConfig.getServerName()));
        context.startActivity(intent);
    }

    /**
     * loginByMainActivity
     */
    private void loginByMainActivity() {
        vcard=new VCard();
        try {
            //todo 要改
            if (XmppConnectionManager.getInstance().getConnection().isConnected()) {
                vcard.load(XmppConnectionManager.getInstance().getConnection(),
                        StringUtil.getJidByName(loginConfig.getUsername(),
                                loginConfig.getServerName()));
            }else
            {
                XmppConnectionManager.getInstance().getConnection().connect();
                vcard.load(XmppConnectionManager.getInstance().getConnection(),
                        StringUtil.getJidByName(loginConfig.getUsername(),
                                loginConfig.getServerName()));
            }
        }catch (XMPPException e)
        {
            Toast.makeText(context,"VCard加载失败",Toast.LENGTH_SHORT).show();
        }
        loginConfig.setAvatar(imageDir+fileName);
        loginConfig.setNickname(vcard.getNickName());
        loginConfig.setProvince(vcard.getAddressFieldHome("province"));
        loginConfig.setCity(vcard.getAddressFieldHome("city"));
        loginConfig.setSign(vcard.getAddressFieldHome("sign"));
        loginConfig.setCountry(vcard.getAddressFieldHome("country"));
        activityTool.saveLoginConfig(loginConfig);
        saveCurrentUser();
        activityTool.startService();
    }

    //获取离线消息
    public static void getOfflineMsg(Context context,String Username){
        //确保用户数据库文件存在
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED) == true) {
            String dbPath = context.getString(R.string.dir)
                    + context.getString(R.string.db_dir) + "/";
            String EXTERN_PATH = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + dbPath;
            File f = new File(EXTERN_PATH+Username);
            if(!f.exists()){
                DataBaseHelper dbhelper = new DataBaseHelper(context, Username, null,3);
                dbhelper.getWritableDatabase();
            }
            //离线消息管理器对象
            OfflineMessageManager offlineMessageManager=new OfflineMessageManager(
                    XmppConnectionManager.getInstance().getConnection());
            try {
                //获取离线消息迭代器
                Iterator<Message> it = offlineMessageManager
                        .getMessages();
                //逐条获取离线消息，并转化成自己定义的消息类型，并保存
                while (it.hasNext()) {
                    Message message = it.next();
                    if(message!=null&&message.getBody()!=null&&!message.getBody().equals("null")){
                        ChatMessage msg=new ChatMessage();
                        //消息时间参数获取
                        String time = (String) message.getProperty("immessage.time");
                        msg.setTime(time);
                        msg.setContent(message.getBody());
                        if (message.getType()==Message.Type.error)
                        {
                            msg.setType(ChatMessage.ERROR);
                        }else
                        {
                            msg.setType(ChatMessage.SUCCESS);
                        }
                        String from=message.getFrom().split("/")[0];
                        msg.setFromSubJid(from);

                        //生成通知
                        NoticeManager noticeManager = NoticeManager
                                .getInstance(context);
                        Notice notice = new Notice();
                        notice.setTitle("会话信息");
                        notice.setNoticeType(Notice.CHAT_MSG);
                        notice.setContent(message.getBody());
                        notice.setFrom(from);
                        notice.setStatus(Notice.UNREAD);
                        notice.setNoticeTime(time);
                        // 历史记录
                        ChatMessage newMessage = new ChatMessage();
                        newMessage.setMsgType(0);
                        newMessage.setFromSubJid(from);
                        newMessage.setContent(message.getBody());
                        newMessage.setTime(time);
                        MessageManager.getInstance(context).saveChatMessage(newMessage);
                        noticeManager.saveNotice(notice);
                    }
                }
                offlineMessageManager.deleteMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
/**
 * Xmpp错误码
 * 302
 * 重定向
 * 尽管HTTP规定中包含八种不同代码来表示重定向，Jabber只用了其中一个（用来代替所有的重定向错误）。
 * 不过Jabber代码302是为以后的功能预留的，目前还没有用到
 * 400
 * 坏请求
 * Jabber代码400用来通知Jabber客户端，一个请求因为其糟糕的语法不能被识别。
 * 例如，当一个Jabber客户端发送一个的订阅请求给它自己活发送一条没有包含“to”属性的消息，Jabber代码400就会产生。
 * 401
 * 未授权的
 * Jabber代码401用来通知Jabber客户端它们提供的是错误的认证信息，如，在登陆一个Jabber服务器时使用一个错误的密码，或未知的用户名。
 * 402
 * 所需的费用
 * Jabber代码402为未来使用进行保留，目前还不用到。
 * 403
 * 禁止
 * Jabber代码403被Jabber服务器用来通知Jabber客户端该客户端的请求可以识别，但服务器拒绝执行。目前只用在注册过程中的密码存储失败。
 * 404
 * 没有找到
 * Jabber代码404用来表明Jabber服务器找不到任何与JabberID匹配的内容，该JabberID是一个Jabber客户端发送消息的目的地。
 * 如，一个用户打算向一个不存在的JabberID发送一条消息。如果接受者的Jabber服务器无法到达，将发送一个来自500级数的错误代码。
 * 405
 * 不允许的
 * Jabber代码405用在不允许操作被’from’地址标识的JabberID。例如，它可能产生在，
 * 一个非管理员用户试图在服务器上发送一条管理员级别的消息，或者一个用户试图发送一台Jabber服务器的时间或版本，
 * 或者发送一个不同的JabberID的vCard。
 * 408
 * 注册超时
 * 当一个Jabber客户端不能在服务器准备好的时间内发起一个请求时，Jabber服务器生成Jabber代码
 * 408。这个代码当前只用于Jabber会话管理器使用的零度认证模式中。
 * 500
 * 服务器内部错误
 * 当一台Jabber服务器遇到一种预期外的条件，该条件阻止服务器处理来自Jabber客户端的包，这是将用到Jabber代码500。
 * 现在，唯一会引发500错误代码的时间是当一个Jabber客户端试图通过服务器认证，而该认证因为某些原因没有被处理（如无法保存密码）。
 * 502
 * 远程服务器错误
 * 当因为无法到达远程服务器导致转发一个包失败时，使用Jabber代码502。
 * 该代码发送的特殊例子包括一个远程服务器的连接的失败，无法获取远程服务器的主机名，以及远程服务器错误导致的外部时间过期。
 * 503
 * 服务无法
 * 当一个Jabber客户端请求一个服务，而Jabber服务器通常由于一些临时原因无法提供该服务时，使获得用Jabber代码503。
 * 例如，一个Jabber客户端试图发送一条消息给另一个用户，该用户不在线，但它的服务器不提供离线存储服务，
 * 服务器将返回一个503错误代码给发送消息的JabberID。当为vcard-temp和jabber:iq:private名字空间设置信息时，
 * 出现通过xdb进行数据存储的写入错误，也使用该代码。
 * 504
 * 远程服务器超时Jabber代码504用于下列情况:试图连接一台服务器发生超时，错误的服务器名。
 * 510
 * 连接失败Jabber代码510
 */