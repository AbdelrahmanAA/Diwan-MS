# WorkManager (SMS sync) creates its Room database by reflection; R8 removed the
# generated WorkDatabase_Impl constructor and the release app crashed at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
