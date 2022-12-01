package com.oxi.g2010.demo;

/**
 * Created by wei.cui on 2017/3/10.
 */

import java.text.SimpleDateFormat;
import java.util.Date;

class OXiDate {

  public static String getDate1() {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      String date = format.format(new Date(System.currentTimeMillis()));
      return date;
  }

  public static String getDate2() {
      SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd_HHmmss");
      String date1 = format1.format(new Date(System.currentTimeMillis()));
      return date1;
  }

}