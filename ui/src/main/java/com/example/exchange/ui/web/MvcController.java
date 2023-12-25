package com.example.exchange.ui.web;

import com.example.exchange.common.ApiException;
import com.example.exchange.common.client.RestClient;
import com.example.exchange.common.UserContext;
import com.example.exchange.common.bean.AuthToken;
import com.example.exchange.common.bean.TransferRequestBean;
import com.example.exchange.common.enums.AssetEnum;
import com.example.exchange.common.enums.UserType;
import com.example.exchange.common.model.ui.UserProfileEntity;
import com.example.exchange.common.support.LoggerSupport;
import com.example.exchange.common.user.UserService;
import com.example.exchange.common.util.HashUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Controller
public class MvcController extends LoggerSupport {

    static Pattern EMAIL = Pattern.compile("^[a-z0-9\\-\\.]+\\@([a-z0-9\\-]+\\.){1,3}[a-z]{2,20}$");
    @Value("#{exchangeConfiguration.hmacKey}")
    String hmacKey;
    @Autowired
    CookieService cookieService;
    @Autowired
    UserService userService;
    @Autowired
    RestClient restClient;
    @Autowired
    Environment environment;

    @PostConstruct
    public void init() {
        if (isLocalDevEnv()) {
            for (int i = 20; i <= 29; i++) {
                String email = "user" + i + "@example.com";
                String name = "User-" + i;
                String password = "password" + i;
                if (userService.fetchUserProfileByEmail(email) == null) {
                    logger.info("auto create user {} for local dev env...", email);
                    doSignup(email, name, password);
                }
            }
        }
    }

    @GetMapping("/")
    public ModelAndView index() {
        if (UserContext.getUserId() == null) {
            return redirect("/signin");
        }
        return prepareModelAndView("index");
    }

    @GetMapping("/signup")
    public ModelAndView signup() {
        if (UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelAndView("signup");
    }

    @PostMapping("/signup")
    public ModelAndView signup(@RequestParam("email") String email, @RequestParam("name") String name,
                               @RequestParam("password") String password) {
        // check email:
        if (email == null || email.isBlank()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid email."));
        }
        email = email.strip().toLowerCase();
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid email."));
        }
        if (userService.fetchUserProfileByEmail(email) != null) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Email exists."));
        }
        // check name:
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid name."));
        }
        name = name.strip();
        // check password:
        if (password == null || password.length() < 8 || password.length() > 32) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid password."));
        }
        doSignup(email, name, password);
        return redirect("/signin");
    }

    @PostMapping(value = "/websocket/token", produces = "application/json")
    @ResponseBody
    String requestWebSocketToken() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "\"\"";
        }
        AuthToken token = new AuthToken(userId, System.currentTimeMillis() + 60_000);
        String strToken = token.toSecureString(hmacKey);
        return "\"" + strToken + "\"";
    }

    /**
     * redirect to sign in page
     *
     * @param request
     * @return
     */
    @GetMapping("/signin")
    public ModelAndView signin(HttpServletRequest request) {
        if (UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelAndView("signin");
    }

    @PostMapping("/signin")
    public ModelAndView signin(@RequestParam("email") String email, @RequestParam("password") String password,
                               HttpServletRequest request, HttpServletResponse response) {
        if (email == null || email.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        }
        if (password == null || password.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        }
        email = email.toLowerCase();

        try {
            UserProfileEntity userProfile = userService.signin(email, password);
            //sign in ok and set cookie
            AuthToken token = new AuthToken(userProfile.userId,
                    System.currentTimeMillis() + 1000 * cookieService.getExpiresInSeconds());
            cookieService.setSessionCookie(request, response, token);
        } catch (ApiException e) {
            logger.warn("sign in failed for " + e.getMessage(), e);
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        } catch (Exception e) {
            logger.warn("sign in failed for " + e.getMessage(), e);
            return prepareModelAndView("signin", Map.of("email", email, "error", "Internal server error."));
        }
        logger.info("sign in ok.");
        return redirect("/");
    }

    @GetMapping("/signout")
    public ModelAndView signout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.deleteSessionCookie(request, response);
        return redirect("/");
    }

    ModelAndView prepareModelAndView(String view, Map<String, Object> model) {
        ModelAndView mv = new ModelAndView(view);
        mv.addAllObjects(model);
        addGlobalModel(mv);
        return mv;
    }

    private ModelAndView prepareModelAndView(String view) {
        ModelAndView mv = new ModelAndView(view);
        addGlobalModel(mv);
        return mv;
    }

    ModelAndView prepareModelAndView(String view, String key, Object value) {
        ModelAndView mv = new ModelAndView(view);
        mv.addObject(key, value);
        addGlobalModel(mv);
        return mv;
    }

    private void addGlobalModel(ModelAndView mv) {
        final Long userId = UserContext.getUserId();
        mv.addObject("__userId__", userId);
        mv.addObject("__profile__", userId == null ? null : userService.getUserProfile(userId));
        mv.addObject("__time__", System.currentTimeMillis());
    }

    private ModelAndView redirect(String url) {
        return new ModelAndView("redirect:" + url);
    }

    private UserProfileEntity doSignup(String email, String name, String password) {
        // sign up
        UserProfileEntity profile = userService.signup(email, name, password);
        // 本地开发环境下自动给用户增加资产:
        if (isLocalDevEnv()) {
            logger.warn("auto deposit assets for user {} in local dev env...", profile.email);
            Random random = new Random(profile.userId);
            deposit(profile.userId, AssetEnum.BTC, new BigDecimal(random.nextInt(5_00, 10_00)).movePointLeft(2));
            deposit(profile.userId, AssetEnum.USD, new BigDecimal(random.nextInt(100000_00, 400000_00)).movePointLeft(2));
        }
        logger.info("user signed up: {}", profile);
        return profile;
    }

    private void deposit(Long userId, AssetEnum asset, BigDecimal amount) {
        var req = new TransferRequestBean();
        req.transferId = HashUtil.sha256(userId + "/" + asset + "/" + amount.stripTrailingZeros().toPlainString()).substring(0, 32);
        req.amount = amount;
        req.asset = asset;
        req.fromUserId = UserType.DEBT.getInternalUserId();
        req.toUserId = userId;
        restClient.post(Map.class, "/internal/transfer", null, req);

    }

    private boolean isLocalDevEnv() {
        return environment.getActiveProfiles().length == 0 && Arrays.equals(environment.getDefaultProfiles(), new String[]{"default"});
    }

}
