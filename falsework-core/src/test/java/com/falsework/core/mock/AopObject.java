package com.falsework.core.mock;

import java.util.Date;

public class AopObject {
    private Object object = new Object();
    private Date date = new Date();

    public void run() {
        System.out.println("run");
    }

    public void print() {
        System.out.println(object);
    }
}
