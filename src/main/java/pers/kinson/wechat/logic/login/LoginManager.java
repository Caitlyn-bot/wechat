package pers.kinson.wechat.logic.login;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import pers.kinson.wechat.base.Constants;
import pers.kinson.wechat.base.SessionManager;
import pers.kinson.wechat.base.UiBaseService;
import pers.kinson.wechat.logic.login.message.req.ReqHeartBeat;
import pers.kinson.wechat.logic.login.message.req.ReqUserLogin;
import pers.kinson.wechat.logic.login.message.res.ResUserLogin;
import pers.kinson.wechat.logic.user.util.PasswordUtil;
import pers.kinson.wechat.net.MessageRouter;
import pers.kinson.wechat.net.message.AbstractPacket;
import pers.kinson.wechat.net.message.PacketType;
import pers.kinson.wechat.ui.R;
import pers.kinson.wechat.ui.StageController;
import pers.kinson.wechat.util.I18n;
import pers.kinson.wechat.util.SchedulerManager;

public class LoginManager {

    private static LoginManager instance = new LoginManager();

    private LoginManager() {
        MessageRouter.INSTANCE.register(PacketType.RespUserLogin.getType(), this::handleLoginResponse);

    }

    public static LoginManager getInstance() {
        return instance;
    }

    /**
     * @param userId
     * @param password 密码明文
     */
    public void beginToLogin(long userId, String password) {
        ReqUserLogin reqLogin = new ReqUserLogin();
        reqLogin.setUserId(userId);
        reqLogin.setUserPwd(PasswordUtil.passwordEncryption(userId, password));
        System.err.println("向服务端发送登录请求");
        SessionManager.INSTANCE.sendMessage(reqLogin);
    }

    public void handleLoginResponse(AbstractPacket packet) {
        ResUserLogin resp = (ResUserLogin) packet;
        boolean isSucc = resp.getIsValid() == Constants.TRUE;
        if (isSucc) {
            UiBaseService.INSTANCE.runTaskInFxThread(() -> {
                redirecToMainPanel();
            });

            registerHeartTimer();
        } else {
            UiBaseService.INSTANCE.runTaskInFxThread(() -> {
                StageController stageController = UiBaseService.INSTANCE.getStageController();
                Stage stage = stageController.getStageBy(R.id.LoginView);
                Pane errPane = (Pane) stage.getScene().getRoot().lookup("#errorPane");
                errPane.setVisible(true);
                Label errTips = (Label) stage.getScene().getRoot().lookup("#errorTips");
                errTips.setText(I18n.get("login.operateFailed"));
            });
        }
    }

    private void redirecToMainPanel() {
        StageController stageController = UiBaseService.INSTANCE.getStageController();
        stageController.switchStage(R.id.MainView, R.id.LoginView);
    }

    /**
     * 注册心跳事件
     */
    private void registerHeartTimer() {
        SchedulerManager.INSTANCE.scheduleAtFixedRate("HEART_BEAT", () -> {
            SessionManager.INSTANCE.sendMessage(new ReqHeartBeat());
        }, 0, 5 * 1000);
    }

}
