package org.zen.iot.data.domain.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.zen.iot.data.base.DTO;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Setter
@Getter
@ToString
public class Account extends DTO {

    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private Set<String> roles;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Account(String username, String password, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }
}
