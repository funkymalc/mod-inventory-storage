package org.folio.rest.impl;

import io.vertx.core.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Mtype;
import org.folio.rest.jaxrs.model.Mtypes;
import org.folio.rest.jaxrs.resource.MaterialTypesResource;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.OutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author shale
 *
 */
public class MaterialTypeAPI implements MaterialTypesResource {

  public static final String MATERIAL_TYPE_TABLE   = "material_type";

  private static final String LOCATION_PREFIX       = "/material-types/";
  private static final Logger log                 = LoggerFactory.getLogger(MaterialTypeAPI.class);
  private final Messages messages                 = Messages.getInstance();
  private String idFieldName                      = "_id";


  public MaterialTypeAPI(Vertx vertx, String tenantId){
    PostgresClient.getInstance(vertx, tenantId).setIdField(idFieldName);
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(MATERIAL_TYPE_TABLE+".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

  @Validate
  @Override
  public void getMaterialTypes(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    /**
    * http://host:port/material-types
    */
    vertxContext.runOnContext(v -> {
      try {
        String tenantId = TenantTool.calculateTenantId( okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT) );
        CQLWrapper cql = getCQL(query, limit, offset);
        PostgresClient.getInstance(vertxContext.owner(), tenantId).get(MATERIAL_TYPE_TABLE, Mtype.class,
          new String[]{"*"}, cql, true, true,
            reply -> {
              try {
                if(reply.succeeded()){
                  Mtypes mtypes = new Mtypes();
                  @SuppressWarnings("unchecked")
                  List<Mtype> mtype = (List<Mtype>) reply.result()[0];
                  mtypes.setMtypes(mtype);
                  mtypes.setTotalRecords((Integer)reply.result()[1]);
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesResponse.withJsonOK(
                    mtypes)));
                }
                else{
                  log.error(reply.cause().getMessage(), reply.cause());
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesResponse
                    .withPlainBadRequest(reply.cause().getMessage())));
                }
              } catch (Exception e) {
                log.error(e.getMessage(), e);
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesResponse
                  .withPlainInternalServerError(messages.getMessage(
                    lang, MessageConsts.InternalServerError))));
              }
            });
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        String message = messages.getMessage(lang, MessageConsts.InternalServerError);
        if(e.getCause() != null && e.getCause().getClass().getSimpleName().endsWith("CQLParseException")){
          message = " CQL parse error " + e.getLocalizedMessage();
        }
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesResponse
          .withPlainInternalServerError(message)));
      }
    });
  }

  @Validate
  @Override
  public void postMaterialTypes(String lang, Mtype entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

    vertxContext.runOnContext(v -> {
      try {
        String id = UUID.randomUUID().toString();
        if(entity.getId() == null){
          entity.setId(id);
        }
        else{
          id = entity.getId();
        }

        String tenantId = TenantTool.calculateTenantId( okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT) );
        PostgresClient.getInstance(vertxContext.owner(), tenantId).save(
          MATERIAL_TYPE_TABLE, id, entity,
          reply -> {
            try {
              if(reply.succeeded()){
                Object ret = reply.result();
                entity.setId((String) ret);
                OutStream stream = new OutStream();
                stream.setData(entity);
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PostMaterialTypesResponse.withJsonCreated(
                  LOCATION_PREFIX + ret, stream)));
              }
              else{
                log.error(reply.cause().getMessage(), reply.cause());
                if(isDuplicate(reply.cause().getMessage())){
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PostMaterialTypesResponse
                    .withJsonUnprocessableEntity(
                      org.folio.rest.tools.utils.ValidationHelper.createValidationErrorMessage(
                        "name", entity.getName(), "Material Type exists"))));
                }
                else{
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PostMaterialTypesResponse
                    .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
              }
            } catch (Exception e) {
              log.error(e.getMessage(), e);
              asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PostMaterialTypesResponse
                .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PostMaterialTypesResponse
          .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    });
  }

  @Validate
  @Override
  public void getMaterialTypesByMaterialtypeId(String materialtypeId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {

    vertxContext.runOnContext(v -> {
      try {
        String tenantId = TenantTool.calculateTenantId( okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT) );

        Criterion c = new Criterion(
          new Criteria().addField(idFieldName).setJSONB(false).setOperation("=").setValue("'"+materialtypeId+"'"));

        PostgresClient.getInstance(vertxContext.owner(), tenantId).get(MATERIAL_TYPE_TABLE, Mtype.class, c, true,
            reply -> {
              try {
                if(reply.succeeded()){
                  @SuppressWarnings("unchecked")
                  List<Mtype> userGroup = (List<Mtype>) reply.result()[0];
                  if(userGroup.isEmpty()){
                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
                      .withPlainNotFound(materialtypeId)));
                  }
                  else{
                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
                      .withJsonOK(userGroup.get(0))));
                  }
                }
                else{
                  log.error(reply.cause().getMessage(), reply.cause());
                  if(isInvalidUUID(reply.cause().getMessage())){
                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
                      .withPlainNotFound(materialtypeId)));
                  }
                  else{
                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
                      .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
                  }
                }
              } catch (Exception e) {
                log.error(e.getMessage(), e);
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
                  .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
        });
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetMaterialTypesByMaterialtypeIdResponse
          .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    });
  }

  @Validate
  @Override
  public void deleteMaterialTypesByMaterialtypeId(String materialtypeId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {

    vertxContext.runOnContext(v -> {
      String tenantId = TenantTool.calculateTenantId( okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT) );
      try {
        Item item = new Item();
        item.setMaterialTypeId(materialtypeId);
        /** check if any item is using this material type **/
        try {
          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(
            ItemStorageAPI.ITEM_TABLE, item, new String[]{idFieldName}, true, false, 0, 1, replyHandler -> {
            if(replyHandler.succeeded()){
              List<Item> mtypeList = (List<Item>) replyHandler.result()[0];
              if(mtypeList.size() > 0){
                String message = "Can not delete material type, "+ materialtypeId + ". " +
                    mtypeList.size()  + " items associated with it";
                log.error(message);
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                  .withPlainBadRequest(message)));
                return;
              }
              else{
                log.info("Attemping delete of unused material type, "+ materialtypeId);
              }
              try {
                PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(MATERIAL_TYPE_TABLE, materialtypeId,
                  reply -> {
                    try {
                      if(reply.succeeded()){
                        if(reply.result().getUpdated() == 1){
                          asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                            .withNoContent()));
                        }
                        else{
                          log.error(messages.getMessage(lang, MessageConsts.DeletedCountError, 1, reply.result().getUpdated()));
                          asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                            .withPlainNotFound(messages.getMessage(lang, MessageConsts.DeletedCountError,1 , reply.result().getUpdated()))));
                        }
                      }
                      else{
                        log.error(reply.cause().getMessage(), reply.cause());
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                          .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
                      }
                    } catch (Exception e) {
                      log.error(e.getMessage(), e);
                      asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                        .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
                    }
                  });
              } catch (Exception e) {
                log.error(e.getMessage(), e);
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                  .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
            else{
              log.error(replyHandler.cause().getMessage(), replyHandler.cause());
              asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
                .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
            .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(DeleteMaterialTypesByMaterialtypeIdResponse
          .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    });
  }

  @Validate
  @Override
  public void putMaterialTypesByMaterialtypeId(String materialtypeId, String lang, Mtype entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {

    vertxContext.runOnContext(v -> {
      String tenantId = TenantTool.calculateTenantId( okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT) );
      try {
        if(entity.getId() == null){
          entity.setId(materialtypeId);
        }
        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
          MATERIAL_TYPE_TABLE, entity, materialtypeId,
          reply -> {
            try {
              if(reply.succeeded()){
                if(reply.result().getUpdated() == 0){
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutMaterialTypesByMaterialtypeIdResponse
                    .withPlainNotFound(messages.getMessage(lang, MessageConsts.NoRecordsUpdated))));
                }
                else{
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutMaterialTypesByMaterialtypeIdResponse
                    .withNoContent()));
                }
              }
              else{
                log.error(reply.cause().getMessage());
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutMaterialTypesByMaterialtypeIdResponse
                  .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            } catch (Exception e) {
              log.error(e.getMessage(), e);
              asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutMaterialTypesByMaterialtypeIdResponse
                .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(PutMaterialTypesByMaterialtypeIdResponse
          .withPlainInternalServerError(messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    });
  }

  @Override
  public void deleteMaterialTypes(String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)
    throws Exception {

    String tenantId = TenantTool.tenantId(okapiHeaders);

    try {
      vertxContext.runOnContext(v -> {
        PostgresClient postgresClient = PostgresClient.getInstance(
          vertxContext.owner(), TenantTool.calculateTenantId(tenantId));

        postgresClient.mutate(String.format("DELETE FROM %s_%s.%s",
          tenantId, "mod_inventory_storage", MATERIAL_TYPE_TABLE),
          reply -> {
            if (reply.succeeded()) {
              asyncResultHandler.handle(Future.succeededFuture(
                MaterialTypesResource.DeleteMaterialTypesResponse.noContent()
                  .build()));
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                MaterialTypesResource.DeleteMaterialTypesResponse.
                  withPlainInternalServerError(reply.cause().getMessage())));
            }
          });
      });
    }
    catch(Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(
        MaterialTypesResource.DeleteMaterialTypesResponse.
          withPlainInternalServerError(e.getMessage())));
    }

  }

  private boolean isDuplicate(String errorMessage){
    if(errorMessage != null && errorMessage.contains("duplicate key value violates unique constraint")){
      return true;
    }
    return false;
  }

  private boolean isInvalidUUID(String errorMessage){
    if(errorMessage != null && errorMessage.contains("invalid input syntax for uuid")){
      return true;
    }
    else{
      return false;
    }
  }

}
