package uk.gov.hmrc.agentsexternalstubs.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User}
import uk.gov.hmrc.agentsexternalstubs.repository.AuthenticatedSessionsRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationService @Inject()(
  authSessionRepository: AuthenticatedSessionsRepository,
  userService: UsersService) {

  def findByAuthToken(authToken: String)(implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] =
    authSessionRepository.findByAuthToken(authToken)

  def createNewAuthentication(userId: String, password: String, providerType: String)(
    implicit ec: ExecutionContext): Future[Option[AuthenticatedSession]] = {
    val authToken = UUID.randomUUID().toString
    for {
      _            <- authSessionRepository.create(userId, authToken, providerType)
      maybeSession <- authSessionRepository.findByAuthToken(authToken)
    } yield maybeSession
  }
}