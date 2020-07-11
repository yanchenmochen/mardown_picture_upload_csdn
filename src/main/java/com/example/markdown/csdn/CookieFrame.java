package com.example.markdown.csdn;

import lombok.Data;

import javax.swing.*;
import java.awt.*;

/**
 * 用于设置与CSDN交互的Cookie的对话框
 *
 * @author songquanheng
 * 2020/7/11-22:17
 */
@Data
public class CookieFrame extends JFrame {
    private String cookieCSDNStr;

    JLabel label = new JLabel("与CSDN交互的cookie字符串");

    //创建JTextField，16表示16列，用于JTextField的宽度显示而不是限制字符个数
    JTextField textField = new JTextField(16);
    JButton button = new JButton("确定");

    //构造函数
    public CookieFrame(String title)
    {
        //继承父类，
        super(title);

        //内容面板
        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        //添加控件
        contentPane.add(label);
        contentPane.add(textField);
        contentPane.add(button);

        //按钮点击处理 lambda表达式
        button.addActionListener((e) -> {
            onButtonOk();
        });
    }

    //事件处理
    private void onButtonOk()
    {
        String str = textField.getText();//获取输入内容
        //判断是否输入了
        if(str.equals(""))
        {
            Object[] options = { "OK ", "CANCEL " };
            JOptionPane.showOptionDialog(null, "您还没有输入 ", "提示", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,null, options, options[0]);
        }
        else {

            cookieCSDNStr = str;
            this.setVisible(false);
        }


    }

}
