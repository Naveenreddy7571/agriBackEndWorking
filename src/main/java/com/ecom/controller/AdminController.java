package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@RestController
@CrossOrigin
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;


	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	@GetMapping("/profiledetailsofuser")
	public ResponseEntity<UserDtls> getUserProfile(Principal p) {
		System.out.println(p);
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			return ResponseEntity.ok(userDtls);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		// m.addAttribute("categorys", categoryService.getAllCategory());
		Page<Category> page = categoryService.getAllCategorPagination(pageNo, pageSize);
		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Category Name already exists");
		} else {

			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				// System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);

		if (deleteCategory) {
			session.setAttribute("succMsg", "category delete success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		Category oldCategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();

		if (!ObjectUtils.isEmpty(category)) {

			oldCategory.setName(category.getName());
			oldCategory.setIsActive(category.getIsActive());
			oldCategory.setImageName(imageName);
		}

		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {

			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				// System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			session.setAttribute("succMsg", "Category update success");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public ResponseEntity<?> saveProduct(@ModelAttribute Product product,
										 @RequestParam("file") MultipartFile image,
										 Principal principal) throws IOException {

		String loggedInAdminEmail = principal.getName();
		UserDtls admin = userService.getUserByEmail(loggedInAdminEmail);


		product.setAddedBy(admin);

		String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());


		Product savedProduct = productService.saveProduct(product);

		if (savedProduct != null) {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator + image.getOriginalFilename());
			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			return ResponseEntity.ok().body("Product saved successfully!");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong on the server");
		}
	}



//	@GetMapping("/products")
//	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
//			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
//			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//
//
//		Page<Product> page = null;
//		if (ch != null && ch.length() > 0) {
//			page = productService.searchProductPagination(pageNo, pageSize, ch);
//		} else {
//			page = productService.getAllProductsPagination(pageNo, pageSize);
//		}
//		m.addAttribute("products", page.getContent());
//
//		m.addAttribute("pageNo", page.getNumber());
//		m.addAttribute("pageSize", pageSize);
//		m.addAttribute("totalElements", page.getTotalElements());
//		m.addAttribute("totalPages", page.getTotalPages());
//		m.addAttribute("isFirst", page.isFirst());
//		m.addAttribute("isLast", page.isLast());
//
//		return "admin/products";
//	}


	@GetMapping("/myproducts")
	public ResponseEntity<?> getProductsForAdmin(Principal principal) {

		String loggedInAdminEmail = principal.getName();
		UserDtls admin = userService.getUserByEmail(loggedInAdminEmail);


		List<Product> products = productService.getProductsByUser(admin);

		return ResponseEntity.ok(products);
	}



	@GetMapping("/deleteProduct/{id}")
	public ResponseEntity<String> deleteProduct(@PathVariable int id) {
		Boolean deleteProduct = productService.deleteProduct(id);

		if (deleteProduct) {
			return ResponseEntity.ok("Product deleted successfully");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to delete product. Something went wrong on the server.");
		}
	}




	@PostMapping("/updateProduct")
	public ResponseEntity<?> updateProduct(
			@ModelAttribute Product product,
			@RequestParam(value = "file", required = false) MultipartFile image) {


		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			return ResponseEntity.badRequest().body("Invalid Discount");
		}


		Product updatedProduct = productService.updateProduct(product, image);

		if (updatedProduct != null) {
			return ResponseEntity.ok("Product updated successfully!");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update the product");
		}
	}






	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		List<UserDtls> users = null;
		if (type == 1) {
			users = userService.getUsers("ROLE_USER");
		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType",type);
		m.addAttribute("users", users);
		return "/admin/users";
	}



	@GetMapping("/orders")
	public ResponseEntity<Map<String, Object>> getAllOrders(
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
		Map<String, Object> response = new HashMap<>();
		response.put("orders", page.getContent());
		response.put("pageNo", page.getNumber());
		response.put("pageSize", pageSize);
		response.put("totalElements", page.getTotalElements());
		response.put("totalPages", page.getTotalPages());
		response.put("isFirst", page.isFirst());
		response.put("isLast", page.isLast());
		response.put("srch", false);  
		return ResponseEntity.ok(response);
	}


	@PostMapping("/update-order-status")
	public ResponseEntity<?> updateOrderStatus(@RequestParam Integer id, @RequestParam String status) {

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			return new ResponseEntity<>("Status updated Sucessfully" , HttpStatus.OK);
		} else {
			return new ResponseEntity<>("status not updated",HttpStatus.BAD_GATEWAY);
		}

	}



//	@GetMapping("/search-order")
//	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
//			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
//			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//
//		if (orderId != null && orderId.length() > 0) {
//
//			ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());
//
//			if (ObjectUtils.isEmpty(order)) {
//				session.setAttribute("errorMsg", "Incorrect orderId");
//				m.addAttribute("orderDtls", null);
//			} else {
//				m.addAttribute("orderDtls", order);
//			}
//
//			m.addAttribute("srch", true);
//		} else {
//
//
//			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
//			m.addAttribute("orders", page);
//			m.addAttribute("srch", false);
//
//			m.addAttribute("pageNo", page.getNumber());
//			m.addAttribute("pageSize", pageSize);
//			m.addAttribute("totalElements", page.getTotalElements());
//			m.addAttribute("totalPages", page.getTotalPages());
//			m.addAttribute("isFirst", page.isFirst());
//			m.addAttribute("isLast", page.isLast());
//
//		}
//		return "/admin/orders";
//
//	}
		@GetMapping("/search-order")
		public ResponseEntity<?> searchProduct(
				@RequestParam String orderId,
				@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
				@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

			if (orderId != null && !orderId.isEmpty()) {
				ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());

				if (ObjectUtils.isEmpty(order)) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body("Incorrect orderId");
				} else {
					return ResponseEntity.ok(order);
				}
			} else {
				Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
				return ResponseEntity.ok(page);
			}
		}




	@PostMapping("/save-admin")
	public ResponseEntity<?> saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file) throws IOException {
		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			return ResponseEntity.ok().body("Registration successful");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong on the server");
		}
	}


	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}


	@PostMapping("/update-profile")
	public ResponseEntity<?> updateProfile(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile img) {
		try {
			UserDtls updatedUserProfile = userService.updateUserProfile(user, img);

			if (ObjectUtils.isEmpty(updatedUserProfile)) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Profile not updated");
			} else {
				return ResponseEntity.ok("Profile Updated");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error updating profile: " + e.getMessage());
		}
	}



	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p) {
		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);
			if (ObjectUtils.isEmpty(updateUser)) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Password not updated. Error in server.");
			} else {
				return ResponseEntity.ok("Password updated successfully");
			}
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current password is incorrect");
		}
	}


}
