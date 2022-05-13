# YModemBleUpdate
YModemBleUpdate OTA Code


update 2022/5/13

更新YmodemDemo 兼容 androidX版本

安卓蓝牙YModem固件升级

原理

1.首先理解什么是YModem通讯？

YModem协议是XModem的改进协议，它最用于调制解调器之间的文件传输的协议，具有快速，稳定传输的优点。它的传输速度比XModem快，这是由于它可以一次传输1024字节的信息块，同时它还支持传输多个文件，也就是常说的批文件传输。 

YModem分成YModem-1K与YModem-g。 

我使用的是YModem-1K 也就是一次传输1024字节。


YModem-1K用1024字节信息块传输取代标准的128字节传输，数据的发送回使用CRC校验，保证数据传输的正确性。它每传输一个信息块数据时，就会等待接收端回应ACK信号，接收到回应后，才会继续传输下一个信息块，保证数据已经全部接收。 


YModem-g传输形式与YModem-1K差不多，但是它去掉了数据的CRC校验码，同时在发送完一个数据块信息后，它不会等待接收端的ACK信号，而直接传输下一个数据块。正是它没有涉及错误校验，才使得它的传输速度比YModem-1K来得块。

一般都会选择YModem-1K传输，平时所说的YModem也是指的是YModem-1K。下面就讲讲它的传输协议 

由于上面都是些 C语言相关的所以省略了直接进入主题。
2.理解传输数据格式
```java
/**
 * ========================================================================================
 * THE YMODEM:
 * Send 0x05>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>* 发送0x05
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< C
 * SOH 00 FF "foo.c" "1064'' NUL[118] CRC CRC >>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< C
 * STX 01 FE data[256] CRC CRC>>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
 * ACK STX 02 FD data[256] CRC CRC>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
 * ACK STX 03 FC data[256] CRC CRC>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * STX 04 FB data[256] CRC CRC>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * SOH 05 FA data[100] 1A[28] CRC CRC>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * EOT >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< NAK
 * EOT>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< C
 * SOH 00 FF NUL[128] CRC CRC >>>>>>>>>>>>>>>>>>>>>>>
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< ACK
 * ===========================================================================================
 **/
```
我们用的设备首先要发送0x05与蓝牙通讯 然后设备返回一个C 接受到C后立即发送头部包到设备 此处 需要CRC16校验 采用标准的欧美标准

接下来就可以依次进行数据发送

注意：每一个公司的协议是不一样的但是你理解原理之后 协议不管怎么改 都可以去解决。
3.使用 YModem

关于YModem的安卓使用百度上搜下来是有很多的Demo，但是关键在于怎么去和蓝牙通讯这一块相当来说比较少了。

推荐的YModem通讯 安卓版本 https://github.com/ArdWang/YModemLib 

里面有一套详细的文件校验，解包，发送bin文件。

4.与蓝牙设备去通讯

关键部分来了 首先通讯的方式 跟蓝牙通讯是一摸一样的
```
 private void startTransmission(){
        //String md5 = MD5Util.MD5(mCurrentFilePath);
        yModem = new YModem.Builder()
                .with(this)
                .filePath(mCurrentFilePath)
                .fileName(mCurrentFileName)
                .checkMd5("")
                .callback(new YModemListener() {
                    @Override
                    public void onDataReady(byte[] data) {
                        if(sendData) {
                            if (bluetoothService.writeCharacteristic(mSendOTACharacteristic, data)) {
                                Log.i("==Send Data is:", bytesToHexFun(data));
                            }
                        }
                    }

                    @Override
                    public void onProgress(int currentSent, int total) {
                        float currentPt = (float)currentSent/total;
                        int a = (int)(currentPt*100);
                        mUpgradeBar.setProgress(currentSent);   // Main Progress
                        mUpgradeBar.setMax(total); // Maximum Progress
                        if(a<=100){
                            mUpgradeBar.setProgressText(""+a+"%");
                        }else{
                            mUpgradeBar.setProgressText("100%");
                        }

                    }

                    @Override
                    public void onSuccess() {
                        Toast.makeText(OTAActivity.this,"固件升级完成",Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailed(String reason) {
                        Toast.makeText(OTAActivity.this,reason,Toast.LENGTH_LONG).show();
                    }
                }).build();
        yModem.start();
    }
```
关键在这段代码中 此段代码放入 连接完成设备的Activity中
startTransmission()要置入 你蓝牙发送数据或者接受数据的代码中，每一个人不一样。我的是发送中。
然后Ymodem会做出反映 你应该把反馈的包值写入到你的蓝牙设备中来进行升级

这一段是写入的代码
```
 public void onDataReady(byte[] data) {
                        if(sendData) {
                            if (bluetoothService.writeCharacteristic(mSendOTACharacteristic, data)) {
                                Log.i("==Send Data is:", bytesToHexFun(data));
                            }
                        }
                    }
 ```
其它的 包扣错误返回，进度等等你要做相应的处理 
具体详细的请查看完整的Demo地址   

点击打开链接

https://github.com/ArdWang/YModemBleUpdate



如果有什么问题 请联系我
