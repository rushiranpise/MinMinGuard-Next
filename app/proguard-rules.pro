# Xposed module entry points are referenced from assets/xposed_init and by name
# from the framework - never rename or strip them.
-keep class tw.fatminmin.xposed.minminguard.next.Main { *; }
-keep class tw.fatminmin.xposed.minminguard.next.blocker.Util { *; }
-keep class tw.fatminmin.xposed.minminguard.next.Common { *; }
-keep class tw.fatminmin.xposed.minminguard.next.FrameworkInfo { *; }
-keep class tw.fatminmin.xposed.minminguard.next.blocker.Blocker { *; }
-keep class tw.fatminmin.xposed.minminguard.next.blocker.adnetwork.** { *; }
-keep class tw.fatminmin.xposed.minminguard.next.blocker.custom_mod.** { *; }

