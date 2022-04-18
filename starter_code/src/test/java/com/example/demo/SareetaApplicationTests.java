package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.ItemController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest
public class SareetaApplicationTests {

	@Autowired
	private CartController cartController;

	@Autowired
	private ItemController itemController;

	@Autowired
	private OrderController orderController;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private UserController userController;

	final String USERNAME = "jbond";

	final String PASSWORD = "frt$d2T2@34";

	@Test
	public void testCreateUser() {
		User user = createUser();
		assertThat(user.getUsername()).isEqualTo(USERNAME);
	}

	@Test
	public void testUserFindById() {
		User user = createUser();
		User existingUser = userController.findById(user.getId()).getBody();
		assertThat(existingUser.getId()).isEqualTo(user.getId());
	}

	@Test
	public void testFindByUserName() {
		User user = createUser();
		User existingUser = userController.findByUserName(user.getUsername()).getBody();
		assertThat(existingUser.getUsername()).isEqualTo(user.getUsername());
	}

	@Test
	public void testUserNotFoundByUsername() {
		ResponseEntity<User> response = userController.findByUserName("ford");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testGetItems() {
		List<Item> existingItems = getExistingItems();
		List<Item> items = itemController.getItems().getBody();
		assertThat(items).isEqualTo(existingItems);
	}

	@Test
	public void testGetItemById() {
		Item existingItem = getExistingItems().get(0);
		Item item = itemController.getItemById(existingItem.getId()).getBody();
		assertThat(item).isEqualTo(existingItem);
	}

	@Test
	public void testGetItemsByName() {
		List<Item> existingItems = getExistingItems().stream().filter(e -> e.getName().equals("Round Widget"))
				.collect(Collectors.toList());
		List<Item> items = itemController.getItemsByName(existingItems.get(0).getName()).getBody();
		assertThat(items).isEqualTo(existingItems);
	}

	@Test
	public void testAddToCart() {
		User user = createUser();
		List<Item> existingItems = getExistingItems().stream().filter(e -> e.getName().equals("Round Widget"))
				.collect(Collectors.toList());
		Cart cart = addToCart(user, existingItems, 1);

		assertThat(cart.getUser().getUsername()).isEqualTo(user.getUsername());
		assertThat(cart.getItems()).isEqualTo(existingItems);
		assertThat(cart.getTotal().doubleValue()).isEqualTo(2.99);
	}

	@Test
	public void testRemoveFromCart() {
		User user = createUser();
		List<Item> existingItems = getExistingItems().stream().filter(e -> e.getName().equals("Round Widget"))
				.collect(Collectors.toList());
		addToCart(user, existingItems, 1);
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername(user.getUsername());
		request.setItemId(existingItems.get(0).getId());
		request.setQuantity(1);

		Cart cart = cartController.removeFromcart(request).getBody();

		assertThat(cart.getUser().getUsername()).isEqualTo(user.getUsername());
		assertThat(cart.getItems().size()).isEqualTo(0);
		assertThat(cart.getTotal().doubleValue()).isEqualTo(0);
	}

	@Test
	public void submit() {
		User user = createUser();
		List<Item> existingItems = getExistingItems().stream().filter(e -> e.getName().equals("Round Widget"))
				.collect(Collectors.toList());

		UserOrder order = submitOrder(user, existingItems, 1);

		assertThat(order.getUser().getUsername()).isEqualTo(user.getUsername());
		assertThat(order.getItems().get(0).getId()).isEqualTo(existingItems.get(0).getId());
		assertThat(order.getTotal().doubleValue()).isEqualTo(2.99);
	}

	@Test
	public void getOrdersForUser() {
		User user = createUser();
		submitOrder(user, new ArrayList<>(Collections.singletonList(getExistingItems().get(0))), 1);
		submitOrder(user, new ArrayList<>(Collections.singletonList(getExistingItems().get(1))), 1);

		List<UserOrder> orders = orderController.getOrdersForUser(user.getUsername()).getBody();

		double total = orders.stream()
				.reduce(0.0, (subTotal, b) -> subTotal + b.getTotal().doubleValue(), Double::sum);

		assertThat(orders.get(0).getUser().getUsername()).isEqualTo(user.getUsername());
		assertThat(orders.size()).isEqualTo(2);
		assertThat(String.format("%.2f", total)).isEqualTo("4.98");
	}

	@Test
	public void testCreateItem() {
		Item newItem = new Item();
		newItem.setName("Banana");
		newItem.setDescription("12 ripe bananas");
		newItem.setPrice(BigDecimal.valueOf(1.50));

		Item savedItem = itemRepository.save(newItem);

		assertThat(savedItem).isEqualTo(newItem);
	}

	private User createUser() {
		CreateUserRequest createUserRequest = new CreateUserRequest();
		createUserRequest.setUsername(USERNAME);
		createUserRequest.setPassword(PASSWORD);
		createUserRequest.setConfirmPassword(PASSWORD);
		return userController.createUser(createUserRequest).getBody();
	}

	private List<Item> getExistingItems() {
		return itemRepository.findAll();
	}

	private Cart addToCart(User user, List<Item> items, int quantity) {
		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername(user.getUsername());
		request.setItemId(items.get(0).getId());
		request.setQuantity(quantity);

		return cartController.addTocart(request).getBody();
	}

	private UserOrder submitOrder(User user, List<Item> items, int quantity) {
		addToCart(user, items, quantity);
		return orderController.submit(user.getUsername()).getBody();
	}

}
