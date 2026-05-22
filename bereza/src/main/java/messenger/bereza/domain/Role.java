package messenger.bereza.domain;

public enum Role {
    TOURIST,
    GUIDE,
    HOTEL,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
