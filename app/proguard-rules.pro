# Xposed module entry points are referenced from assets/xposed_init and by name
# from the framework - never rename or strip them.
-keep class tw.fatminmin.xposed.minminguard.Main { *; }
-keep class tw.fatminmin.xposed.minminguard.blocker.Util { *; }
-keep class tw.fatminmin.xposed.minminguard.Common { *; }
-keep class tw.fatminmin.xposed.minminguard.FrameworkInfo { *; }
-keep class tw.fatminmin.xposed.minminguard.blocker.Blocker { *; }
-keep class tw.fatminmin.xposed.minminguard.blocker.adnetwork.** { *; }
-keep class tw.fatminmin.xposed.minminguard.blocker.custom_mod.** { *; }

