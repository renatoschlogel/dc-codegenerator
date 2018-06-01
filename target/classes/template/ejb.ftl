<#assign className = class.className>
<#assign company = class.company.packageName>
<#assign module = class.moduleBasic?lower_case>
<#assign eao = module + "EAO.get" + class.entityName + "EAO()">
<#assign gerenciador = "gerenciador" + class.entityName>
package ${class.package};

import java.util.Optional;
import java.util.List;

import javax.ejb.Stateless;

import com.${company}.cooperate.${module}.server.eao.${module?cap_first}EAO;
import com.${company}.cooperate.${module}.common.remote.${class.entityName}Remote;
import com.${company}.cooperate.${module}.common.entities.${class.entityName};
import com.${company}.cooperate.${module}.common.consultas.${class.entityName}VO;
import com.datacoper.cooperate.arquitetura.common.beans.BeanConsultaGroup;
import com.datacoper.cooperate.arquitetura.common.beans.PageResult;
import com.datacoper.cooperate.arquitetura.common.exception.DCLogicException;
import com.datacoper.cooperate.arquitetura.common.exception.DCRuntimeException;
import com.datacoper.cooperate.arquitetura.common.persistence.entities.EntityState;
<#list class.imports as import>
import ${import};
</#list>

@Stateless
public class ${className} implements ${class.entityName}Remote {

    private ${module?cap_first}EAO ${module}EAO = new ${module?cap_first}EAO();
    
    private Gerenciador${class.entityName} gerenciador = new Gerenciador${class.entityName}();
    
    <#include "defaultConstructor.ftl">    

    @Override
    public Optional<${class.entityName}> find(Long id) {
        return ${eao}.findOptional(id);
    }

    @Override
    public Optional<${class.entityName}> findFetch(Long id) {
        return ${eao}.findFetch(id);
    }

    @Override
    public PageResult<${class.entityName}VO> find(BeanConsultaGroup consultaGroup) {
        return ${eao}.find(consultaGroup);
    }

    @Override
    public List<${class.entityName}> find(List<Long> ids) {
        return ${eao}.find(ids);
    }

    @Override
    public Long confirm(${class.entityName} ${class.entityName?uncap_first}) {
        try {
            return gerenciador.confirm(${class.entityName?uncap_first}).getId();
        } catch (DCLogicException e) {
            throw new DCRuntimeException(e);
        }
    }

    @Override
    public void excluir(Long id) {
        ${class.entityName} ${class.entityName?uncap_first} = ${eao}.find(id);
        ${class.entityName?uncap_first}.setEntityState(EntityState.DELETED);

        confirm(${class.entityName?uncap_first});
    }
}