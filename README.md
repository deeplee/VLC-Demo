
本Demo实际与ARM-V7a编译的

String path = "rtsp://172.16.32.14:8554/h263ESVideoTest"; 是网络串流地址

如果播放本地视频请使用

mLocation = LibVLC.PathToURI("/sdcard/boot.mp4");转义为VLC可识别的Uri
