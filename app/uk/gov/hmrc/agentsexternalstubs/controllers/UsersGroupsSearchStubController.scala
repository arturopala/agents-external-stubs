package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.UsersGroupsSearchStubController.{GetGroupResponse, GetUserResponse}
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class UsersGroupsSearchStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService)
    extends BaseController with CurrentSession {

  def getUser(userId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(userId, session.planetId).map {
        case Some(user) => NonAuthoritativeInformation(RestfulResponse(GetUserResponse.from(user)))
        case None       => NotFound("USER_NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def getGroup(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByGroupId(groupId, session.planetId)(100)
        .map(s =>
          s.find(_.isAgent).orElse(s.find(_.isOrganisation)).orElse(s.headOption) match {
            case Some(user) =>
              NonAuthoritativeInformation(
                RestfulResponse(
                  GetGroupResponse.from(groupId, user),
                  Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(groupId).url)))
            case None => NotFound("GROUP_NOT_FOUND")
        })
    }(SessionRecordNotFound)
  }

  def getGroupUsers(groupId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByGroupId(groupId, session.planetId)(100)
        .map {
          case users if users.isEmpty =>
            NotFound("GROUP_NOT_FOUND")
          case users =>
            NonAuthoritativeInformation(RestfulResponse(users))
        }
    }(SessionRecordNotFound)
  }

  def getGroupByAgentCode(agentCode: String, agentId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService
        .findByAgentCode(agentCode, session.planetId)(100)
        .map(s =>
          s.find(_.isAgent).orElse(s.find(_.isOrganisation)).orElse(s.headOption) match {
            case Some(user) =>
              NonAuthoritativeInformation(RestfulResponse(
                GetGroupResponse.from(user.groupId.getOrElse(""), user),
                Link("users", routes.UsersGroupsSearchStubController.getGroupUsers(user.groupId.getOrElse("")).url)
              ))
            case None => NotFound("GROUP_NOT_FOUND")
        })
    }(SessionRecordNotFound)
  }

}

object UsersGroupsSearchStubController {

  /**
    * {
    *     "userId": ":userId",
    *     "name": "Subscribed MTD Agent",
    *     "email": "default@email.com",
    *     "affinityGroup": "Agent",
    *     "agentCode": "LMNOPQ234568",
    *     "agentFriendlyName": "MTD Agency",
    *     "agentId": "?",
    *     "credentialRole": "User",
    *     "description": "ManualUserCreation",
    *     "groupId": "04389535-78F7-4213-9169-FD0DD3553731"
    * }
    */
  case class GetUserResponse(
    name: String,
    userId: Option[String] = None,
    email: Option[String] = None,
    affinityGroup: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None,
    credentialRole: Option[String] = None,
    description: Option[String] = None,
    groupId: Option[String] = None,
    owningUserId: Option[String] = None)

  object GetUserResponse {
    implicit val writes: Writes[GetUserResponse] = Json.writes[GetUserResponse]

    def from(user: User): GetUserResponse =
      GetUserResponse(
        name = user.name.getOrElse(""),
        userId = Some(user.userId),
        affinityGroup = user.affinityGroup,
        agentCode = user.agentCode,
        agentFriendlyName = user.agentFriendlyName,
        agentId = user.agentId,
        credentialRole = user.credentialRole,
        groupId = user.groupId
      )
  }

  /**
    * {
    *   "_links": [
    *     { "rel": "users", "link": "/groups/:groupdId/users" }
    *   ],
    *   "groupId": ":groupId",
    *   "affinityGroup": "Agent",
    *   "agentCode": "NQJUEJCWT14",
    *   "agentFriendlyName": "JoeBloggs",
    *   "agentId": "?" //missing in GsoAdminGetUserDetailsByGroupId
    * }
    */
  case class GetGroupResponse(
    groupId: Option[String] = None,
    affinityGroup: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None)

  object GetGroupResponse {
    implicit val writes: Writes[GetGroupResponse] = Json.writes[GetGroupResponse]

    def from(groupId: String, user: User): GetGroupResponse =
      GetGroupResponse(
        groupId = Some(groupId),
        affinityGroup = user.affinityGroup,
        agentCode = user.agentCode,
        agentFriendlyName = user.agentFriendlyName,
        agentId = user.agentId)
  }

}