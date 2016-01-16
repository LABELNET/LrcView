echo "# LrcView" >> README.md
git init
git add README.md
git commit -m "first commit"
git remote add origin https://github.com/LABELNET/LrcView.git
git push -u origin master


实现总共分为控制模块和显示模块： 首先，说明下：


通过是示例图，了解和弦，歌词，和弦键；

控制模块：通过音量键进行控制和弦键，从而实现歌词的移动和和弦键的切换，在切换和弦键的时候，需要改变和弦键的颜色和大小，已完成； 例如：



显示模块：和弦键必须和歌词对应，在那个字上必须对照;

# # 目前所剩下的任务：为显示模块，和弦键与字对照，无法实现；


https://github.com/LABELNET/LrcView/wiki/LrcView
