module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires miglayout;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires java.sql;
    requires java.desktop;
    opens com.udacity.catpoint.security.data to com.google.gson;
}