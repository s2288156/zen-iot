package org.zen.iot.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.zen.iot.dao.model.AccountEntity;
import org.zen.iot.data.domain.account.Account;

/**
 * @author Wu.Chunyang
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Account entityToSimpleAccount(AccountEntity entity);
}
