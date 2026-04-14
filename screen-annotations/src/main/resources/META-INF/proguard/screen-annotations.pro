# Screen Compiler Plugin — Consumer ProGuard/R8 Rules
#
# The @Screen compiler plugin generates Class.forName() calls and
# FragmentFactory.instantiate() calls with hardcoded fully qualified
# class name strings. R8 name obfuscation changes class names at
# build time while the hardcoded strings remain unchanged, causing
# ClassNotFoundException at runtime.

# Keep the @Screen annotation so R8 can evaluate the keep rule below.
-keep class com.adkhambek.screen.Screen

# Keep class names of @Screen-annotated Fragments.
# Generated code references these by name via Class.forName() and
# FragmentFactory.instantiate(), both of which resolve classes by string.
-keep @com.adkhambek.screen.Screen class *

# Note: If your @Screen(arg = MyArg::class) argument type is a custom
# Parcelable that is not otherwise kept (e.g. by @Parcelize or the
# default Android Parcelable rules), add a keep rule for it as well:
#   -keep class com.example.MyArg
