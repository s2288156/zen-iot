package org.zeniot.data.domain.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.zeniot.data.base.DTO;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Getter
@Setter
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

}
