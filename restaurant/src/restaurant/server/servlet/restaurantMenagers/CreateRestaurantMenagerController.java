package restaurant.server.servlet.restaurantMenagers;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import restaurant.externals.HashPassword;
import restaurant.externals.ResultCode;
import restaurant.server.entity.Address;
import restaurant.server.entity.Image;
import restaurant.server.entity.Restaurant;
import restaurant.server.entity.RestaurantType;
import restaurant.server.entity.Street;
import restaurant.server.entity.User;
import restaurant.server.entity.UserType;
import restaurant.server.session.AddressDaoLocal;
import restaurant.server.session.ImageDaoLocal;
import restaurant.server.session.RestaurantDaoLocal;
import restaurant.server.session.RestaurantTypeDaoLocal;
import restaurant.server.session.StreetDaoLocal;
import restaurant.server.session.UserDaoLocal;
import restaurant.server.session.UserTypeDaoLocal;

public class CreateRestaurantMenagerController extends HttpServlet {

	private static final long serialVersionUID = -1085613434690860205L;

	@EJB
	private UserDaoLocal userDao;
	@EJB
	private ImageDaoLocal imageDao;
	@EJB
	private UserTypeDaoLocal userTypeDao;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		if (req.getSession().getAttribute("user") == null) {
			System.out.println("Nema korisnika na sesiji");
			resp.sendRedirect(resp.encodeRedirectURL("../../login.jsp"));
			return;
		} else {
			User user = (User) req.getSession().getAttribute("user");
			System.out.println("User type: " + user.getUserType().getName());
			if (!(user.getUserType().getName()).equals("SYSTEM_MENAGER")) {
				System.out.println("Korisnik nije menadzer sistema i nema ovlascenja da uradi tako nesto!");
				resp.sendRedirect(resp.encodeRedirectURL("../../insufficient_privileges.jsp"));
				return;
			}
			ObjectMapper resultMapper = new ObjectMapper();
			String userEmail = "";
			String userPassword = "";
			String userName = "";
			String userSurname = "";
			ObjectMapper mapper = new ObjectMapper();
			HashMap<String, String> data = mapper.readValue(req.getParameter("registrationData"), HashMap.class);
			for (String key : data.keySet()) {
				if (key.equals("userEmail"))
					userEmail = data.get(key);
				else if (key.equals("userPassword"))
					userPassword = data.get(key);
				else if (key.equals("userName"))
					userName = data.get(key);
				else if (key.equals("userSurname"))
					userSurname = data.get(key);
			}

			if (userEmail.equals("") || userEmail == null || userPassword.equals("") || userPassword == null
					|| userName.equals("") || userName == null || userSurname.equals("") || userSurname == null) {

				resp.setContentType("application/json; charset=utf-8");
				PrintWriter out = resp.getWriter();
				resultMapper.writeValue(out, ResultCode.REGISTER_USER_FIELD_EMPTY.toString());
				return;
			}

			List<User> users = userDao.findAll();
			/* Proveriti da li je korisnik vec registrovan i ima nalog */
			for (User u : users) {
				if (u.getEmail().equals(userEmail)) {
					resp.setContentType("application/json; charset=utf-8");
					PrintWriter out = resp.getWriter();
					resultMapper.writeValue(out, ResultCode.REGISTER_USER_ALREADY_EXISTS.toString());
					return;
				}
			}

			String uploadImageRealName = (String) req.getSession().getAttribute("uploadImageRealName");
			byte[] uploadImageHashedName = (byte[]) req.getSession().getAttribute("uploadImageHashedName");
			String uploadImagePath = (String) req.getSession().getAttribute("uploadImagePath");
			if (uploadImageRealName != null) {
				Image image = new Image();
				image.setName(uploadImageHashedName);
				image.setRealName(uploadImageRealName);
				image.setPath(uploadImagePath);

				imageDao.persist(image);

				User rm = new User();

				rm.setName(userName);
				rm.setSurname(userSurname);
				rm.setEmail(userEmail);
				byte[] salt = HashPassword.getNextSalt();
				byte[] hashedId = HashPassword.hashPassword(HashPassword.strToChar(userPassword), salt);
				rm.setSalt(salt);
				rm.setPassword(hashedId);
				rm.setActivated(true);
				rm.setIsSessionActive(false);

				List<UserType> userTypes = userTypeDao.findAll();

				for (UserType userType : userTypes) {
					if (userType.getName().equals("RESTAURANT_MENAGER")) {
						rm.setUserType(userType);
						User persisted = userDao.persist(rm);
						if (persisted == null) {
							resp.setContentType("application/json; charset=utf-8");
							PrintWriter out = resp.getWriter();
							resultMapper.writeValue(out, ResultCode.REGISTER_USER_ERROR.toString());
							return;
						}
						image.setUser(rm);
						imageDao.merge(image);
						rm.setImage(image);

						byte[] tokenSalt = ByteBuffer.allocate(4).putInt(rm.getId()).array();
						byte[] token = HashPassword.hashPassword(HashPassword.strToChar(rm.getEmail()), tokenSalt);
						rm.setToken(token);
						rm.setActivated(true);
						rm.setSystemMenager(user);

						userDao.merge(rm);
						userTypeDao.merge(userType);
						userType.add(rm);
						userTypeDao.merge(userType);
						break;

					}
				}
			} else {
				User rm = new User();

				rm.setName(userName);
				rm.setSurname(userSurname);
				rm.setEmail(userEmail);
				byte[] salt = HashPassword.getNextSalt();
				byte[] hashedId = HashPassword.hashPassword(HashPassword.strToChar(userPassword), salt);
				rm.setSalt(salt);
				rm.setPassword(hashedId);

				rm.setIsSessionActive(false);

				List<UserType> userTypes = userTypeDao.findAll();

				for (UserType userType : userTypes) {
					if (userType.getName().equals("RESTAURANT_MENAGER")) {
						rm.setUserType(userType);
						User persisted = userDao.persist(rm);
						if (persisted == null) {
							resp.setContentType("application/json; charset=utf-8");
							PrintWriter out = resp.getWriter();
							resultMapper.writeValue(out, ResultCode.REGISTER_USER_ERROR.toString());
							return;
						}

						byte[] tokenSalt = ByteBuffer.allocate(4).putInt(rm.getId()).array();
						byte[] token = HashPassword.hashPassword(HashPassword.strToChar(rm.getEmail()), tokenSalt);
						rm.setToken(token);
						rm.setActivated(true);
						rm.setSystemMenager(user);

						userDao.merge(rm);
						userTypeDao.merge(userType);
						userType.add(rm);
						userTypeDao.merge(userType);
						break;

					}
				}
			}
			resp.setContentType("application/json; charset=utf-8");
			PrintWriter out = resp.getWriter();
			resultMapper.writeValue(out, "USPEH");
			return;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(req, resp);
	}

	private boolean persistUser(String name, String surname, String email, String password, HttpServletRequest req,
			Address address) {
		try {
			User restaurantMenager2 = new User();
			restaurantMenager2.setName(name);
			restaurantMenager2.setSurname(surname);
			restaurantMenager2.setEmail(email);
			restaurantMenager2.setActivated(true);
			List<UserType> userTypes = userTypeDao.findAll();
			for (UserType type : userTypes) {
				if (type.getName().equals("RESTAURANT_MENAGER")) {
					restaurantMenager2.setUserType(type);
					break;
				}
			}

			byte[] salt = new byte[16];
			byte[] hashedPassword = new byte[256];
			salt = HashPassword.getNextSalt();
			char[] passJan = HashPassword.strToChar(password);
			hashedPassword = HashPassword.hashPassword(passJan, salt);

			restaurantMenager2.setPassword(hashedPassword);
			restaurantMenager2.setSalt(salt);
			User user = (User) req.getSession().getAttribute("user");
			restaurantMenager2.setSystemMenager(user);
			restaurantMenager2.setAddress(address);

			String realName = (String) req.getSession().getAttribute("uploadImageRealName");
			if (realName != null) {
				byte[] hashedName = (byte[]) req.getSession().getAttribute("uploadImageHashedName");
				String path = (String) req.getSession().getAttribute("uploadImagePath");
				Image image = new Image();
				image.setName(hashedName);
				image.setPath(path);
				image.setRealName(realName);
				imageDao.persist(image);
				user.setImage(image);
			}

			userDao.persist(restaurantMenager2);

			user.add(restaurantMenager2);
			userDao.merge(user);

			if (realName != null) {
				restaurantMenager2.getImage().setUser(restaurantMenager2);
				imageDao.merge(restaurantMenager2.getImage());
			}
			return true;
		} catch (Exception ex) {
			return false;
		}

	}
}
