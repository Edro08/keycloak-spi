package utils.errors;

public class Catalog_Errors {
    Errors errors = new Errors();

    public Errors ErrInternalServer = errors.NewError(
            500,
            "internal server error");


    public Errors ErrUserNotFound = errors.NewError(
            400,
            "user not found");


    public Errors ErrUserInvalidEmail = errors.NewError(
            400,
            "user invalid email");

    public Errors ErrRedirectUri = errors.NewError(
            500,
            "redirect uri not found");

    public Errors ErrLifeSpan = errors.NewError(
            500,
            "lifespan not found");
}
