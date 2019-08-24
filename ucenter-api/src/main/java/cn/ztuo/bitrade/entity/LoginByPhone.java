package cn.ztuo.bitrade.entity;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

/**
 * @author GuoShuai
 * @date 2018年01月08日
 */
@Data
public class LoginByPhone {
    /**
     * 大陆手机号码11位数，匹配格式：前三位固定格式+后8位任意数
     * 此方法中前三位格式有：
     * 13+任意数
     * 15+除4的任意数
     * 18+任意数
     * 17+任意数
     * 147
     */
    /*@Pattern(regexp = "^((13[0-9])|(15[^4])|(18[0-9])|(17[0-9])|(147))\\d{8}$", message = "{LoginByPhone.phone.pattern}")*/
    @NotBlank(message = "{LoginByPhone.phone.null}")
    private String phone;

    //@Pattern(regexp="^(?=.*?[a-zA-Z])(?=.*?[0-9])[a-zA-Z0-9]{6,20}$",message = "{LoginByEmail.password.Pattern}")

    @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[\\s\\S]{6,20}$",message = "{LoginByEmail.password.Pattern}")
    private String password;

    @NotBlank(message = "{LoginByPhone.username.null}")
    @Length(min = 3, max = 20, message = "{LoginByPhone.username.length}")
    private String username;

    @NotBlank(message = "{LoginByPhone.country.null}")
    private String country;


    @NotBlank(message = "{LoginByPhone.code.null}")
    private String code;

    @NotBlank(message = "{LoginByPhone.ticket.null}")
    private String ticket;

    @NotBlank(message = "{LoginByPhone.randStr.null}")
    private String randStr;

    private String promotion;
}
