package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@CrossOrigin
public class HomeController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CartService cartService;

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




    @GetMapping("/allproducts")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();

        // Convert to ProductDto to include the full image URL
        List<Product> productDtos = products.stream().map(product -> {
            Product productDto = new Product();
            productDto.setId(product.getId());
            productDto.setTitle(product.getTitle());
            productDto.setDescription(product.getDescription());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setStock(product.getStock());
            productDto.setDiscount(product.getDiscount());
            productDto.setDiscountPrice(product.getDiscountPrice());
            productDto.setIsActive(product.getIsActive());

            // Full URL to the image
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/img/product_img/")
                    .path(product.getImage())
                    .toUriString();
            productDto.setImage(imageUrl);

            return productDto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(productDtos);
    }

//
//	@GetMapping("/allproducts")
//	public ResponseEntity<List<Product>> getAllProducts(){
//		return ResponseEntity<List<Product>>(productService.getAllProducts(),HttpStatus.OK);
//	}
//@GetMapping("/allproducts")
//public ResponseEntity<List<Product>> getAllProducts() {
//	List<Product> products = productService.getAllProducts();
//	return new ResponseEntity<>(products, HttpStatus.OK);
//}
//	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
//			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
//			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize,
//			@RequestParam(defaultValue = "") String ch) {
//
//		List<Category> categories = categoryService.getAllActiveCategory();
//		m.addAttribute("paramValue", category);
//		m.addAttribute("categories", categories);
//
////		List<Product> products = productService.getAllActiveProducts(category);
////		m.addAttribute("products", products);
//		Page<Product> page = null;
//		if (StringUtils.isEmpty(ch)) {
//			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
//		} else {
//			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
//		}
//
//		List<Product> products = page.getContent();
//		m.addAttribute("products", products);
//		m.addAttribute("productsSize", products.size());
//
//		m.addAttribute("pageNo", page.getNumber());
//		m.addAttribute("pageSize", pageSize);
//		m.addAttribute("totalElements", page.getTotalElements());
//		m.addAttribute("totalPages", page.getTotalPages());
//		m.addAttribute("isFirst", page.isFirst());
//		m.addAttribute("isLast", page.isLast());
//
//		return "product";
//	}

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> product(@PathVariable int id) {

        System.out.println("came to get the individuL PRODUCT");
        System.out.println(id);
        Product productById = productService.getProductById(id);
        //m.addAttribute("product", productById);
        return ResponseEntity.ok(productById);
    }

    @PostMapping("/saveUser")
    public ResponseEntity<String> saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file) throws IOException {
        Boolean existsEmail = userService.existsEmail(user.getEmail());

        if (existsEmail) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        } else {
            String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
            user.setProfileImage(imageName);
            UserDtls saveUser = userService.saveUser(user);

            if (!ObjectUtils.isEmpty(saveUser)) {
                if (!file.isEmpty()) {
                    File saveFile = new ClassPathResource("static/img").getFile();
                    Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator + file.getOriginalFilename());
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                }
                return ResponseEntity.status(HttpStatus.OK).body("Register successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong on the server");
            }
        }
    }


//	Forgot Password Code 

//	@GetMapping("/forgot-password")
//	public String showForgotPassword() {
//		return "forgot_password.html";
//	}
//
//	@PostMapping("/forgot-password")
//	public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request)
//			throws UnsupportedEncodingException, MessagingException {
//
//		UserDtls userByEmail = userService.getUserByEmail(email);
//
//		if (ObjectUtils.isEmpty(userByEmail)) {
//			session.setAttribute("errorMsg", "Invalid email");
//		} else {
//
//			String resetToken = UUID.randomUUID().toString();
//			userService.updateUserResetToken(email, resetToken);
//
//			// Generate URL :
//			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg
//
//			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;
//
//			Boolean sendMail = commonUtil.sendMail(url, email);
//
//			if (sendMail) {
//				session.setAttribute("succMsg", "Please check your email..Password Reset link sent");
//			} else {
//				session.setAttribute("errorMsg", "Somethong wrong on server ! Email not send");
//			}
//		}
//
//		return "redirect:/forgot-password";
//	}
//
//	@GetMapping("/reset-password")
//	public String showResetPassword(@RequestParam String token, HttpSession session, Model m) {
//
//		UserDtls userByToken = userService.getUserByToken(token);
//
//		if (userByToken == null) {
//			m.addAttribute("msg", "Your link is invalid or expired !!");
//			return "message";
//		}
//		m.addAttribute("token", token);
//		return "reset_password";
//	}
//
//	@PostMapping("/reset-password")
//	public String resetPassword(@RequestParam String token, @RequestParam String password, HttpSession session,
//			Model m) {
//
//		UserDtls userByToken = userService.getUserByToken(token);
//		if (userByToken == null) {
//			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
//			return "message";
//		} else {
//			userByToken.setPassword(passwordEncoder.encode(password));
//			userByToken.setResetToken(null);
//			userService.updateUser(userByToken);
//			// session.setAttribute("succMsg", "Password change successfully");
//			m.addAttribute("msg", "Password change successfully");
//
//			return "message";
//		}
//
//	}

    @GetMapping("/search")
    public String searchProduct(@RequestParam String ch, Model m) {
        List<Product> searchProducts = productService.searchProduct(ch);
        m.addAttribute("products", searchProducts);
        List<Category> categories = categoryService.getAllActiveCategory();
        m.addAttribute("categories", categories);
        return "product";

    }

}
