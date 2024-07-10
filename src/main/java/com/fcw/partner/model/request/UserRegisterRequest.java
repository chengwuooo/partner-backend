package com.fcw.partner.model.request;

import lombok.Data;

import java.io.Serializable;


@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userName;
    private String userAccount;
    private String userPassword;
    private String checkPassword;

}
