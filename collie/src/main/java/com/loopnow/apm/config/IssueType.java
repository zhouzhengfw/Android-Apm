package com.loopnow.apm.config;
public enum IssueType{
     FPS(0),
     ANR(1),
     TRAFFIC(2),
     LEAK(3),
     MEMORYCOST(4),
     APPLAUNCH(5),
     ACTIVITYLAUNCH(6),
     BATTERYCOST(7),
     KOOM(8);
     private  int type;
     private IssueType(int type){
        this.type = type;
     }

     public int getType() {
         return type;
     }
 }
