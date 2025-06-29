package utils.errors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

// @JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Errors {

    @JsonProperty("description")
    private String description;

    @JsonProperty("code")
    private int code;

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }

    public Errors NewError(int code, String description){
        Errors errors = new Errors();
        errors.setCode(code);
        errors.setDescription(description);
        return errors;
    }
}
