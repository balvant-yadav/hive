/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.parse.authorization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.PrincipalType;
import org.apache.hadoop.hive.ql.ErrorMsg;
import org.apache.hadoop.hive.ql.exec.Task;
import org.apache.hadoop.hive.ql.exec.TaskFactory;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.Partition;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer;
import org.apache.hadoop.hive.ql.parse.DDLSemanticAnalyzer;
import org.apache.hadoop.hive.ql.parse.HiveParser;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.plan.DDLWork;
import org.apache.hadoop.hive.ql.plan.GrantDesc;
import org.apache.hadoop.hive.ql.plan.GrantRevokeRoleDDL;
import org.apache.hadoop.hive.ql.plan.PrincipalDesc;
import org.apache.hadoop.hive.ql.plan.PrivilegeDesc;
import org.apache.hadoop.hive.ql.plan.PrivilegeObjectDesc;
import org.apache.hadoop.hive.ql.plan.RevokeDesc;
import org.apache.hadoop.hive.ql.plan.RoleDDLDesc;
import org.apache.hadoop.hive.ql.plan.ShowGrantDesc;
import org.apache.hadoop.hive.ql.security.authorization.Privilege;
import org.apache.hadoop.hive.ql.security.authorization.PrivilegeRegistry;
import org.apache.hadoop.hive.ql.security.authorization.PrivilegeType;
import org.apache.hadoop.hive.ql.session.SessionState;
/**
 * Default implementation of HiveAuthorizationTaskFactory
 */
@SuppressWarnings("unchecked")
public class HiveAuthorizationTaskFactoryImpl implements HiveAuthorizationTaskFactory {

  private final HiveConf conf;
  private final Hive db;

  public HiveAuthorizationTaskFactoryImpl(HiveConf conf, Hive db) {
    this.conf = conf;
    this.db = db;
  }

  @Override
  public Task<? extends Serializable> createCreateRoleTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) {
    String roleName = BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(0).getText());
    RoleDDLDesc roleDesc = new RoleDDLDesc(roleName, RoleDDLDesc.RoleOperation.CREATE_ROLE);
    return TaskFactory.get(new DDLWork(inputs, outputs, roleDesc), conf);
  }
  @Override
  public Task<? extends Serializable> createDropRoleTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) {
    String roleName = BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(0).getText());
    RoleDDLDesc roleDesc = new RoleDDLDesc(roleName, RoleDDLDesc.RoleOperation.DROP_ROLE);
    return TaskFactory.get(new DDLWork(inputs, outputs, roleDesc), conf);
  }
  @Override
  public Task<? extends Serializable> createShowRoleGrantTask(ASTNode ast, Path resultFile,
      HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs) {
    ASTNode child = (ASTNode) ast.getChild(0);
    PrincipalType principalType = PrincipalType.USER;
    switch (child.getType()) {
    case HiveParser.TOK_USER:
      principalType = PrincipalType.USER;
      break;
    case HiveParser.TOK_GROUP:
      principalType = PrincipalType.GROUP;
      break;
    case HiveParser.TOK_ROLE:
      principalType = PrincipalType.ROLE;
      break;
    }
    String principalName = BaseSemanticAnalyzer.unescapeIdentifier(child.getChild(0).getText());
    RoleDDLDesc roleDesc = new RoleDDLDesc(principalName, principalType,
        RoleDDLDesc.RoleOperation.SHOW_ROLE_GRANT, null);
    roleDesc.setResFile(resultFile.toString());
    return TaskFactory.get(new DDLWork(inputs, outputs,  roleDesc), conf);
  }
  @Override
  public Task<? extends Serializable> createGrantTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) throws SemanticException {
    List<PrivilegeDesc> privilegeDesc = analyzePrivilegeListDef(
        (ASTNode) ast.getChild(0));
    List<PrincipalDesc> principalDesc = AuthorizationParseUtils.analyzePrincipalListDef(
        (ASTNode) ast.getChild(1));
    boolean grantOption = false;
    PrivilegeObjectDesc privilegeObj = null;

    if (ast.getChildCount() > 2) {
      for (int i = 2; i < ast.getChildCount(); i++) {
        ASTNode astChild = (ASTNode) ast.getChild(i);
        if (astChild.getType() == HiveParser.TOK_GRANT_WITH_OPTION) {
          grantOption = true;
        } else if (astChild.getType() == HiveParser.TOK_PRIV_OBJECT) {
          privilegeObj = analyzePrivilegeObject(astChild, outputs);
        }
      }
    }

    String userName = SessionState.getUserFromAuthenticator();

    GrantDesc grantDesc = new GrantDesc(privilegeObj, privilegeDesc,
        principalDesc, userName, PrincipalType.USER, grantOption);
    return TaskFactory.get(new DDLWork(inputs, outputs, grantDesc), conf);
  }
  @Override
  public Task<? extends Serializable> createRevokeTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) throws SemanticException {
    List<PrivilegeDesc> privilegeDesc = analyzePrivilegeListDef((ASTNode) ast.getChild(0));
    List<PrincipalDesc> principalDesc = AuthorizationParseUtils.analyzePrincipalListDef((ASTNode) ast.getChild(1));
    PrivilegeObjectDesc hiveObj = null;
    if (ast.getChildCount() > 2) {
      ASTNode astChild = (ASTNode) ast.getChild(2);
      hiveObj = analyzePrivilegeObject(astChild, outputs);
    }
    RevokeDesc revokeDesc = new RevokeDesc(privilegeDesc, principalDesc, hiveObj);
    return TaskFactory.get(new DDLWork(inputs, outputs, revokeDesc), conf);
  }
  @Override
  public Task<? extends Serializable> createGrantRoleTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) {
    return analyzeGrantRevokeRole(true, ast, inputs, outputs);
  }
  @Override
  public Task<? extends Serializable> createShowGrantTask(ASTNode ast, Path resultFile, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) throws SemanticException {

    PrincipalDesc principalDesc = null;
    PrivilegeObjectDesc privHiveObj = null;
    List<String> cols = null;

    ASTNode param = null;
    if (ast.getChildCount() > 0) {
      param = (ASTNode) ast.getChild(0);
      principalDesc = AuthorizationParseUtils.getPrincipalDesc(param);
      if (principalDesc != null) {
        param = (ASTNode) ast.getChild(1);  // shift one
      }
    }

    if (param != null) {
      if (param.getType() == HiveParser.TOK_RESOURCE_ALL) {
        privHiveObj = new PrivilegeObjectDesc();
      } else if (param.getType() == HiveParser.TOK_PRIV_OBJECT_COL) {
        privHiveObj = new PrivilegeObjectDesc();
        //set object name
        String text = param.getChild(0).getText();
        privHiveObj.setObject(BaseSemanticAnalyzer.unescapeIdentifier(text));
        //set object type
        ASTNode objTypeNode = (ASTNode) param.getChild(1);
        privHiveObj.setTable(objTypeNode.getToken().getType() == HiveParser.TOK_TABLE_TYPE);

        //set col and partition spec if specified
        for (int i = 2; i < param.getChildCount(); i++) {
          ASTNode partOrCol = (ASTNode) param.getChild(i);
          if (partOrCol.getType() == HiveParser.TOK_PARTSPEC) {
            privHiveObj.setPartSpec(DDLSemanticAnalyzer.getPartSpec(partOrCol));
          } else if (partOrCol.getType() == HiveParser.TOK_TABCOLNAME) {
            cols = BaseSemanticAnalyzer.getColumnNames(partOrCol);
          } else {
            throw new SemanticException("Invalid token type " + partOrCol.getType());
          }
        }
      }
    }

    ShowGrantDesc showGrant = new ShowGrantDesc(resultFile.toString(),
        principalDesc, privHiveObj, cols);
    return TaskFactory.get(new DDLWork(inputs, outputs, showGrant), conf);
  }

  @Override
  public Task<? extends Serializable> createRevokeRoleTask(ASTNode ast, HashSet<ReadEntity> inputs,
      HashSet<WriteEntity> outputs) {
    return analyzeGrantRevokeRole(false, ast, inputs, outputs);
  }
  private Task<? extends Serializable> analyzeGrantRevokeRole(boolean isGrant, ASTNode ast,
      HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs) {
    List<PrincipalDesc> principalDesc = AuthorizationParseUtils.analyzePrincipalListDef(
        (ASTNode) ast.getChild(0));

    //check if admin option has been specified
    int rolesStartPos = 1;
    ASTNode wAdminOption = (ASTNode) ast.getChild(1);
    boolean isAdmin = false;
    if(wAdminOption.getToken().getType() == HiveParser.TOK_GRANT_WITH_ADMIN_OPTION){
      rolesStartPos = 2; //start reading role names from next postion
      isAdmin = true;
    }

    List<String> roles = new ArrayList<String>();
    for (int i = rolesStartPos; i < ast.getChildCount(); i++) {
      roles.add(BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(i).getText()));
    }

    String roleOwnerName = SessionState.getUserFromAuthenticator();

    //until change is made to use the admin option. Default to false with V2 authorization


    GrantRevokeRoleDDL grantRevokeRoleDDL = new GrantRevokeRoleDDL(isGrant,
        roles, principalDesc, roleOwnerName, PrincipalType.USER, isAdmin);
    return TaskFactory.get(new DDLWork(inputs, outputs, grantRevokeRoleDDL), conf);
  }

  private PrivilegeObjectDesc analyzePrivilegeObject(ASTNode ast,
      HashSet<WriteEntity> outputs)
      throws SemanticException {

    PrivilegeObjectDesc subject = new PrivilegeObjectDesc();
    //set object identifier
    subject.setObject(BaseSemanticAnalyzer.unescapeIdentifier(ast.getChild(0).getText()));
    //set object type
    ASTNode objTypeNode =  (ASTNode) ast.getChild(1);
    subject.setTable(objTypeNode.getToken().getType() == HiveParser.TOK_TABLE_TYPE);
    if (ast.getChildCount() == 3) {
      //if partition spec node is present, set partition spec
      ASTNode partSpecNode = (ASTNode) ast.getChild(2);
      subject.setPartSpec(DDLSemanticAnalyzer.getPartSpec(partSpecNode));
    }

    if (subject.getTable()) {
      Table tbl = getTable(SessionState.get().getCurrentDatabase(), subject.getObject());
      if (subject.getPartSpec() != null) {
        Partition part = getPartition(tbl, subject.getPartSpec());
        outputs.add(new WriteEntity(part, WriteEntity.WriteType.DDL_METADATA_ONLY));
      } else {
        outputs.add(new WriteEntity(tbl, WriteEntity.WriteType.DDL_METADATA_ONLY));
      }
    }

    return subject;
  }

  private List<PrivilegeDesc> analyzePrivilegeListDef(ASTNode node)
      throws SemanticException {
    List<PrivilegeDesc> ret = new ArrayList<PrivilegeDesc>();
    for (int i = 0; i < node.getChildCount(); i++) {
      ASTNode privilegeDef = (ASTNode) node.getChild(i);
      ASTNode privilegeType = (ASTNode) privilegeDef.getChild(0);
      Privilege privObj = PrivilegeRegistry.getPrivilege(privilegeType.getType());

      if (privObj == null) {
        throw new SemanticException("Undefined privilege " + PrivilegeType.
            getPrivTypeByToken(privilegeType.getType()));
      }
      List<String> cols = null;
      if (privilegeDef.getChildCount() > 1) {
        cols = BaseSemanticAnalyzer.getColumnNames((ASTNode) privilegeDef.getChild(1));
      }
      PrivilegeDesc privilegeDesc = new PrivilegeDesc(privObj, cols);
      ret.add(privilegeDesc);
    }
    return ret;
  }

  private Table getTable(String database, String tblName)
      throws SemanticException {
    try {
      Table tab = database == null ? db.getTable(tblName, false)
          : db.getTable(database, tblName, false);
      if (tab == null) {
        throw new SemanticException(ErrorMsg.INVALID_TABLE.getMsg(tblName));
      }
      return tab;
    } catch (HiveException e) {
      if(e instanceof SemanticException) {
        throw (SemanticException)e;
      }
      throw new SemanticException(ErrorMsg.INVALID_TABLE.getMsg(tblName), e);
    }
  }

  private Partition getPartition(Table table, Map<String, String> partSpec)
      throws SemanticException {
    try {
      Partition partition = db.getPartition(table, partSpec, false);
      if (partition == null) {
        throw new SemanticException(toMessage(ErrorMsg.INVALID_PARTITION, partSpec));
      }
      return partition;
    } catch (HiveException e) {
      if(e instanceof SemanticException) {
        throw (SemanticException)e;
      }
      throw new SemanticException(toMessage(ErrorMsg.INVALID_PARTITION, partSpec), e);
    }

  }
  private String toMessage(ErrorMsg message, Object detail) {
    return detail == null ? message.getMsg() : message.getMsg(detail.toString());
  }

  @Override
  public Task<? extends Serializable> createSetRoleTask(String roleName,
      HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs)
      throws SemanticException {
    return TaskFactory.get(new DDLWork(inputs, outputs, new RoleDDLDesc(roleName,
      RoleDDLDesc.RoleOperation.SET_ROLE)), conf);
  }

  @Override
  public Task<? extends Serializable> createShowCurrentRoleTask(
      HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs, Path resFile)
      throws SemanticException {
    RoleDDLDesc ddlDesc = new RoleDDLDesc(null, RoleDDLDesc.RoleOperation.SHOW_CURRENT_ROLE);
    ddlDesc.setResFile(resFile.toString());
    return TaskFactory.get(new DDLWork(inputs, outputs, ddlDesc), conf);
  }
}
