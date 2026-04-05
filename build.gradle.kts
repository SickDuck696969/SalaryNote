plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false

    // THÊM DÒNG NÀY VÀO ĐÂY (Phiên bản phải khớp với 1.9.20 ở trên):
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}